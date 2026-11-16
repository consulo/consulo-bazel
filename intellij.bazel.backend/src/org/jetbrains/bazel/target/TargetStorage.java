package org.jetbrains.bazel.target;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.CommonWorkspaceSyncConfig;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class TargetStorage {
    // consulo: upstream public top-level class CachedSnapshotView of TargetStorage.kt is nested here
    public static class CachedSnapshotView {
        private final WorkspaceSnapshot mySnapshot;
        private final Set<WorkspaceTargetKey> myImportKeys;
        private final Map<Label, WorkspaceTargetKey> myLabel2Key;
        private final OutputLocationResolver myOutputLocationResolver;

        public CachedSnapshotView(WorkspaceSnapshot snapshot) {
            mySnapshot = snapshot;

            @Nullable CommonWorkspaceSyncConfig config = snapshot.syncConfigs().stream()
                .filter(CommonWorkspaceSyncConfig.class::isInstance)
                .map(CommonWorkspaceSyncConfig.class::cast)
                .findFirst()
                .orElse(null);
            if (config == null) {
                myImportKeys = Set.of();
                myLabel2Key = Map.of();
            }
            else {
                // keep previous behavior, only targets at specific depth (from root targets) are imported
                // consulo: the upstream default arguments runtimeDependencies = maxDepth < 0, condition = { true } are passed explicitly
                myImportKeys = snapshot.targetGraph()
                    .findAllTargetsAtDepth(config.importDepth(), config.importDepth() < 0, true, key -> true)
                    .collect(Collectors.toCollection(HashSet::new));

                // precompute label -> key map for compatibility purposes
                Map<Label, WorkspaceTargetKey> label2Key = new HashMap<>(myImportKeys.size());
                for (WorkspaceTargetKey key : myImportKeys) {
                    // TODO: keep this heuristic until UX features are aware of full target key
                    boolean keyIsPlain = key.aspectIds().equals(WorkspaceAspectIds.EMPTY);
                    if (label2Key.get(key.label()) == null || keyIsPlain) {
                        label2Key.put(key.label(), key);
                    }
                }
                this.myLabel2Key = label2Key;
            }
            myOutputLocationResolver = DefaultOutputLocationResolver.createExecrootResolving(snapshot.bazelInfo());

            myTarget2Info = snapshot.targets().allTargetInfos()
                .filter(it -> myImportKeys.contains(it.key()))
                .collect(Collectors.toMap(BuildTargetInfo::key, it -> it, (first, second) -> second, LinkedHashMap::new));

            myInfos = new ArrayList<>(myTarget2Info.values());

            Map<Label, BuildTargetInfo> label2Info = new LinkedHashMap<>();
            myLabel2Key.forEach((label, key) -> {
                @Nullable BuildTargetInfo info = myTarget2Info.get(key);
                if (info != null) {
                    label2Info.put(label, info);
                }
            });
            myLabel2Info = label2Info;
        }

        public WorkspaceSnapshot getSnapshot() {
            return mySnapshot;
        }

        public Set<WorkspaceTargetKey> getImportKeys() {
            return myImportKeys;
        }

        public Map<Label, WorkspaceTargetKey> getLabel2Key() {
            return myLabel2Key;
        }

        public OutputLocationResolver getOutputLocationResolver() {
            return myOutputLocationResolver;
        }

        private final Map<WorkspaceTargetKey, BuildTargetInfo> myTarget2Info;

        private final List<BuildTargetInfo> myInfos;

        public List<BuildTargetInfo> getInfos() {
            return myInfos;
        }

        private final Map<Label, BuildTargetInfo> myLabel2Info;

        public Map<Label, BuildTargetInfo> getLabel2Info() {
            return myLabel2Info;
        }

        private final LazyValue<List<String>> myShortLabels = LazyValue.notNull(() ->
            getInfos().stream()
                .map(it -> BazelRepoMappingUtils.toShortString(it.id(), getSnapshot().repoMapping()))
                .toList()
        );

        public List<String> getShortLabels() {
            return myShortLabels.get();
        }

        private final LazyValue<List<String>> myExecutableShortLabels = LazyValue.notNull(() ->
            getInfos().stream()
                .filter(it -> it.kind().isExecutable())
                .map(it -> BazelRepoMappingUtils.toShortString(it.id(), getSnapshot().repoMapping()))
                .toList()
        );

        public List<String> getExecutableShortLabels() {
            return myExecutableShortLabels.get();
        }

        private final LazyValue<List<BuildTargetInfo>> myAllTestableInfos = LazyValue.notNull(() ->
            getInfos().stream().filter(it -> it.kind().ruleType() == RuleType.TEST && !BuildTargetInfo.isManual(it)).toList()
        );

        public List<BuildTargetInfo> getAllTestableInfos() {
            return myAllTestableInfos.get();
        }

        public List<Label> targetsForPath(Path path) {
            return mySnapshot.fileToTarget().getTargetsByFile(path).stream()
                .filter(myImportKeys::contains)
                .map(WorkspaceTargetKey::label)
                .distinct()
                .toList();
        }

        public List<Label> executableTargetsFor(Label label) {
            @Nullable ResolvedLabel canonicalLabel = BazelRepoMappingUtils.toCanonicalLabelOrThis(label, mySnapshot.repoMapping());
            return canonicalLabel != null ? mySnapshot.executableTargets().executableTargetsFor(canonicalLabel) : List.of();
        }

        // small data cache to accelerate repeated bulk reads of the same target data
        // consulo: Caffeine is not available in Consulo, the Guava cache is used instead
        private final Cache<Pair<Label, Class<?>>, BuildTargetData> myDataCache = CacheBuilder.newBuilder()
            .maximumSize(64)
            .build();

        public <T extends BuildTargetData> @Nullable T data(Label label, Class<T> type) {
            BuildTargetData cached;
            try {
                cached = myDataCache.get(Pair.<Label, Class<?>>create(label, type), () -> {
                    @Nullable WorkspaceTargetKey key = myLabel2Key.get(label);
                    if (key == null) {
                        return EmptyBuildTargetData.INSTANCE;
                    }
                    @Nullable T data = mySnapshot.targets().findTargetData(key, type);
                    return data != null ? data : EmptyBuildTargetData.INSTANCE;
                });
            }
            catch (ExecutionException e) {
                throw new IllegalStateException(e.getCause());
            }
            if (cached == EmptyBuildTargetData.INSTANCE) {
                return null;
            }
            else {
                return type.cast(cached);
            }
        }
    }

    private final Project myProject;
    private final WorkspaceSnapshotService mySnapshotService;

    private final ObservableValue<CachedSnapshotView> myViews;

    @Inject
    public TargetStorage(Project project) {
        myProject = project;
        mySnapshotService = project.getInstance(WorkspaceSnapshotService.class);
        // consulo: StateFlow.mapStateIn(coroutineScope) is ObservableValue.map, the service coroutine scope is not needed
        myViews = mySnapshotService.getSnapshot().map(CachedSnapshotView::new);
    }

    public ObservableValue<CachedSnapshotView> getViews() {
        return myViews;
    }

    public CompletableFuture<?> awaitLoaded() {
        return mySnapshotService.currentSnapshot() // force load
            // compare with the latest snapshot: `views` is conflated and can skip the one that `currentSnapshot()` returned
            // consulo: ObservableValue.map updates `views` synchronously on every snapshot change, so after currentSnapshot()
            // `views` already holds the view of the latest snapshot (upstream: views.first { it.snapshot === snapshot.value })
            .thenApply(ignored -> myViews.get());
    }

    private CachedSnapshotView view() {
        return myViews.get();
    }

    @Nullable Label canonicalLabel(Label label) {
        return BazelRepoMappingUtils.toCanonicalLabelOrThis(label, myProject);
    }

    public List<String> getAllTargetShortLabels() {
        return view().getShortLabels();
    }

    public List<String> getAllExecutableTargetLabels() {
        return view().getExecutableShortLabels();
    }

    public void addFileToTargetIdEntry(Path file, Collection<Label> targets) {
        CachedSnapshotView view = view();
        view.getSnapshot().fileToTarget().addMapping(
            file,
            targets.stream().map(it -> view.getLabel2Key().get(it)).filter(Objects::nonNull).toList()
        );
    }

    public void removeFileToTargetIdEntry(Path file) {
        view().getSnapshot().fileToTarget().removeMapping(file);
    }

    public Stream<Label> allTargets() {
        return view().getInfos().stream().map(BuildTargetInfo::id);
    }

    public int getTotalTargetCount() {
        return view().getInfos().size();
    }

    public List<Label> getTargetsForPath(Path path) {
        return view().targetsForPath(path);
    }

    public List<Label> getTargetsForFile(VirtualFile file) {
        @Nullable Path path = toNioPathOrNull(file);
        return path != null ? getTargetsForPath(path) : List.of();
    }

    public List<Label> getExecutableTargetsForFile(VirtualFile file) {
        @Nullable Path path = toNioPathOrNull(file);
        if (path == null) {
            return List.of();
        }
        CachedSnapshotView view = view();
        List<Label> targetsForFile = view.targetsForPath(path);
        List<Label> executableDirectTargets = targetsForFile.stream().filter(label -> {
            @Nullable BuildTargetInfo info = view.getLabel2Info().get(label);
            return info != null && info.kind().isExecutable();
        }).toList();
        if (executableDirectTargets.isEmpty()) {
            return targetsForFile.stream().flatMap(it -> view.executableTargetsFor(it).stream()).distinct().toList();
        }
        return executableDirectTargets;
    }

    public List<Label> getExecutableTargetsForTarget(Label target) {
        return view().executableTargetsFor(target);
    }

    public boolean isLibrary(Label target) {
        @Nullable BuildTargetInfo info = getTargetInfo(this, target);
        return info != null && info.kind().ruleType() == RuleType.LIBRARY;
    }

    public <T extends BuildTargetData> @Nullable T getTargetDataForLabel(Label label, Class<T> type) {
        @Nullable Label canonicalLabel = canonicalLabel(label);
        return canonicalLabel != null ? view().data(canonicalLabel, type) : null;
    }

    public int getTotalFileCount() {
        return view().getSnapshot().fileToTarget().getSize();
    }

    public RepoMapping repoMapping() {
        return view().getSnapshot().repoMapping();
    }

    public @Nullable Path resolveExecrootOutputLocation(OutputLocation location) {
        CachedSnapshotView view = view();
        return view.getOutputLocationResolver().resolve(location, RepoMapping.getLocalRepositories(view.getSnapshot().repoMapping()));
    }

    // consulo: the upstream reified extension `TargetStorage.getTargetDataForLabel<T>(label)` is the member
    // getTargetDataForLabel(label, type)

    public static @Nullable BuildTargetInfo getTargetInfo(TargetStorage receiver, Label label) {
        @Nullable Label canonicalLabel = receiver.canonicalLabel(label);
        return canonicalLabel != null ? receiver.myViews.get().getLabel2Info().get(canonicalLabel) : null;
    }

    public static List<BuildTargetInfo> allTargetInfos(TargetStorage receiver) {
        return receiver.myViews.get().getInfos();
    }

    public static List<BuildTargetInfo> allTestableTargetInfos(TargetStorage receiver) {
        return receiver.myViews.get().getAllTestableInfos();
    }

    public static @Nullable BuildTarget getBuildTarget(TargetStorage receiver, Label label) {
        CachedSnapshotView view = receiver.myViews.get();
        @Nullable Label canonicalLabel = receiver.canonicalLabel(label);
        @Nullable WorkspaceTargetKey key = canonicalLabel != null ? view.getLabel2Key().get(canonicalLabel) : null;
        if (key == null) {
            return null;
        }
        return view.getSnapshot().targets().findTargetByKey(key);
    }

    public static TargetStorage getTargetStorage(Project receiver) {
        return receiver.getInstance(TargetStorage.class);
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}

final class EmptyBuildTargetData implements BuildTargetData {
    static final EmptyBuildTargetData INSTANCE = new EmptyBuildTargetData();

    private EmptyBuildTargetData() {
    }

    @Override
    public String toString() {
        return "EmptyBuildTargetData";
    }
}
