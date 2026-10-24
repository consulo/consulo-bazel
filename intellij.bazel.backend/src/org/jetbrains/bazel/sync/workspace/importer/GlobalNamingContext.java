package org.jetbrains.bazel.sync.workspace.importer;

import consulo.logging.Logger;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.magicmetamodel.NameProviderTools;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface GlobalNamingContext {
    @Nullable String find(NameSpace space, NameProducer producer, WorkspaceTargetKey key);

    String findOrFallback(NameSpace space, NameProducer producer, WorkspaceTargetKey key);

    enum NameSpace {
        MODULE, LIBRARY
    }

    /**
     * Name producer, usually each language will provide its own
     * <p>
     * A name claimed by more than one producer goes to none of them, each falls back to an {@code id} suffixed name instead
     *
     * @param id Unique language identifier e.g., jvm, python
     */
    record NameProducer(String id) {
    }

    interface GlobalNamingContextBuilder {
        /**
         * Declare name in global naming context, after resolution it can be retrieved
         * using {@link GlobalNamingContext#find} or {@link GlobalNamingContext#findOrFallback}
         *
         * @param space    Namespace, each namespace is resolved separately
         * @param producer Unique producer owning name
         * @param key      Key used to derive name during resolve
         */
        void declare(NameSpace space, NameProducer producer, WorkspaceTargetKey key, @Nullable String preferred);

        default void declare(NameSpace space, NameProducer producer, WorkspaceTargetKey key) {
            declare(space, producer, key, null);
        }

        GlobalNamingContext build();

        static GlobalNamingContextBuilder create(RepoMapping repoMapping) {
            return new GlobalNamingContextBuilderImpl(repoMapping);
        }
    }
}

record NameRequest(GlobalNamingContext.NameSpace space, GlobalNamingContext.NameProducer producer, WorkspaceTargetKey key) {
}

class GlobalNamingContextBuilderImpl implements GlobalNamingContext.GlobalNamingContextBuilder {
    private final RepoMapping myRepoMapping;
    private final Set<NameRequest> myDeclared = new LinkedHashSet<>();
    private final Map<NameRequest, String> myPreferred = new HashMap<>();

    GlobalNamingContextBuilderImpl(RepoMapping repoMapping) {
        myRepoMapping = repoMapping;
    }

    @Override
    public void declare(
        GlobalNamingContext.NameSpace space,
        GlobalNamingContext.NameProducer producer,
        WorkspaceTargetKey key,
        @Nullable String preferred
    ) {
        NameRequest request = new NameRequest(space, producer, key);
        myDeclared.add(request);
        if (preferred != null) {
            myPreferred.putIfAbsent(request, preferred);
        }
    }

    @Override
    public GlobalNamingContext build() {
        Map<NameRequest, String> names = new LinkedHashMap<>();
        // namespaces are separate, so resolve them on their own
        for (GlobalNamingContext.NameSpace space : GlobalNamingContext.NameSpace.values()) {
            List<NameRequest> requests = new ArrayList<>();
            for (NameRequest it : myDeclared) {
                if (it.space() == space) {
                    requests.add(it);
                }
            }
            if (!requests.isEmpty()) {
                requests.sort(GlobalNamingContextSupport.REQUEST_ORDER);
                names.putAll(resolve(requests));
            }
        }
        return new ResolvedNamingContext(myRepoMapping, names);
    }

    // worklist based algorithm for efficient conflict resolution between requested names
    // overall idea is simple, each `NameRequest` provides a list of lazy name candidates,
    // then we're trying each candidate for `NameRequest` until one is not taken, move to next candidate for
    // all requests and go through worklist until each name is resolved, as fallback hash-based names are selected
    private Map<NameRequest, String> resolve(List<NameRequest> ordered) {
        Map<NameRequest, List<Supplier<String>>> candidates = new LinkedHashMap<>();
        for (NameRequest it : ordered) {
            candidates.put(it, GlobalNamingContextSupport.candidates(myRepoMapping, it, myPreferred.get(it)));
        }
        Map<NameRequest, String> result = new HashMap<>();
        Set<String> taken = new HashSet<>();

        // most recent candidates
        Map<NameRequest, String> lastName = new HashMap<>();

        List<NameRequest> pending = ordered;
        int index = 0;
        while (!pending.isEmpty()) {
            // being absent from this map means request has run out of candidates
            Map<NameRequest, String> nameOf = new LinkedHashMap<>();
            for (NameRequest request : pending) {
                List<Supplier<String>> requestCandidates = Objects.requireNonNull(candidates.get(request));
                if (index >= requestCandidates.size()) {
                    continue;
                }
                String name = requestCandidates.get(index).get();
                nameOf.put(request, name);
                lastName.put(request, name);
            }

            // assign fallback names, usually shouldn't happen with the default candidates
            // but is kept here to provide soundness of name resolution
            for (NameRequest request : pending) {
                if (!nameOf.containsKey(request)) {
                    String fallback = GlobalNamingContextSupport.fallbackName(request, Objects.requireNonNull(lastName.get(request)), taken);
                    result.put(request, fallback);
                    taken.add(fallback);
                }
            }

            List<NameRequest> stillPending = new ArrayList<>();
            // a name goes to the request that is the only one asking for it, everyone else moves to the next worklist
            // this is mainly about targets shared between languages, e.g. a target imported as both a jvm and a python
            // module leaves the plain name behind and both end up producer suffixed
            Map<String, List<NameRequest>> groups = new LinkedHashMap<>();
            for (Map.Entry<NameRequest, String> entry : nameOf.entrySet()) {
                groups.computeIfAbsent(entry.getValue(), it -> new ArrayList<>()).add(entry.getKey());
            }
            for (Map.Entry<String, List<NameRequest>> entry : groups.entrySet()) {
                String name = entry.getKey();
                List<NameRequest> group = entry.getValue();
                NameRequest winner = group.size() == 1 && !taken.contains(name) ? group.get(0) : null;
                if (winner != null) {
                    result.put(winner, name);
                    taken.add(name);
                }
                for (NameRequest it : group) {
                    if (it != winner) {
                        stillPending.add(it);
                    }
                }
            }
            // swap worklist and move to next candidate
            pending = stillPending;
            index++;
        }
        return result;
    }
}

class ResolvedNamingContext implements GlobalNamingContext {
    private final RepoMapping myRepoMapping;
    private final Map<NameRequest, String> myNames;
    private final Map<NameSpace, Set<String>> myTaken = new HashMap<>();
    private final Map<NameRequest, String> myFallbacks = new ConcurrentHashMap<>();

    ResolvedNamingContext(RepoMapping repoMapping, Map<NameRequest, String> names) {
        myRepoMapping = repoMapping;
        myNames = names;
        for (Map.Entry<NameRequest, String> entry : names.entrySet()) {
            myTaken.computeIfAbsent(entry.getKey().space(), it -> new HashSet<>()).add(entry.getValue());
        }
    }

    @Override
    public @Nullable String find(NameSpace space, NameProducer producer, WorkspaceTargetKey key) {
        return myNames.get(new NameRequest(space, producer, key));
    }

    @Override
    public String findOrFallback(NameSpace space, NameProducer producer, WorkspaceTargetKey key) {
        String found = find(space, producer, key);
        if (found != null) {
            return found;
        }
        return myFallbacks.computeIfAbsent(new NameRequest(space, producer, key), request ->
            GlobalNamingContextSupport.fallbackName(
                request,
                GlobalNamingContextSupport.candidates(myRepoMapping, request, null).get(0).get(),
                myTaken.getOrDefault(space, Set.of())
            )
        );
    }
}

// consulo: upstream file-private top-level functions and values
final class GlobalNamingContextSupport {
    private static final Logger LOG = Logger.getInstance(GlobalNamingContext.class);

    static final Comparator<NameRequest> REQUEST_ORDER =
        Comparator.<NameRequest, String>comparing(it -> it.producer().id())
            .thenComparing(it -> it.key().label())
            .thenComparing(it -> it.key().configuration().getShortChecksum(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(it -> String.join(",", it.key().aspectIds().getIds()));

    private GlobalNamingContextSupport() {
    }

    static List<Supplier<String>> candidates(RepoMapping repoMapping, NameRequest request, @Nullable String preferred) {
        WorkspaceTargetKey key = request.key();
        String producer = request.producer().id();
        Supplier<String> plain;
        Supplier<String> decorated;
        switch (request.space()) {
            case MODULE -> {
                plain = () -> NameProviderTools.formatAsModuleName(key, repoMapping, false);
                decorated = () -> NameProviderTools.formatAsModuleName(key, repoMapping, true);
            }
            case LIBRARY -> {
                plain = () -> NameProviderTools.formatAsLibraryName(key, repoMapping, false);
                decorated = () -> NameProviderTools.formatAsLibraryName(key, repoMapping, true);
            }
            default -> throw new IllegalStateException();
        }
        // intellij.bazel.commons   preferred, only when one was declared
        // a.b                      plain
        // a.b-python               plain + producer
        // a.b-a1b2c3d              plain + configuration
        // a.b-a1b2c3d-python       plain + configuration + producer
        List<Supplier<String>> result = new ArrayList<>();
        if (preferred != null) {
            result.add(() -> preferred);
        }
        result.add(plain);
        result.add(() -> plain.get() + "-" + producer);
        result.add(decorated);
        result.add(() -> decorated.get() + "-" + producer);
        return result;
    }

    static String fallbackName(NameRequest request, String base, Set<String> taken) {
        String decorated = base + "-" + StringUtils.md5Hash(request.producer().id() + "|" + request.key(), 5);
        if (!taken.contains(decorated)) {
            return decorated;
        }
        LOG.warn("naming collision disambiguation failed for " + request + ", falling back to counter name");
        for (int i = 1; ; i++) {
            String name = decorated + "-" + i;
            if (!taken.contains(name)) {
                return name;
            }
        }
    }
}
