package org.jetbrains.bazel.ignore;

import consulo.annotation.component.ExtensionImpl;
import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.event.MessageEvent;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ControlFlowException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingProvider;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.ProjectSyncHook;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@ServiceImpl
@Singleton
public class DefaultBazelIgnoreService implements BazelIgnoreService {
    public static final Logger LOG = Logger.getInstance(DefaultBazelIgnoreService.class);

    private final Project myProject;

    // We are not interested in live updates since Bazel uses ignore only during sync
    private final AtomicReference<@Nullable State> myState = new AtomicReference<>(null);

    @Inject
    public DefaultBazelIgnoreService(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isIgnored(Path path) {
        State state = getOrUpdate();
        if (state == null) {
            return false;
        }

        Path absolutePath;
        if (path.isAbsolute()) {
            if (!path.startsWith(state.getWorkspaceRoot())) {
                return false;
            }
            absolutePath = path;
        }
        else {
            absolutePath = state.getWorkspaceRoot().resolve(path);
        }

        Map.Entry<Path, List<BazelIgnoreMatcher>> matchers = matchesFor(state, absolutePath);
        if (matchers == null) {
            return false;
        }
        Path relativePath = relativeToOrNull(absolutePath, matchers.getKey());
        if (relativePath == null) {
            return false;
        }
        for (BazelIgnoreMatcher it : matchers.getValue()) {
            if (it.match(relativePath)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable State getOrUpdate() {
        State state = this.myState.get();
        if (state != null) {
            return state;
        }

        VirtualFile projectRootDir = myProject.getInstance(BazelProjectInfoService.class).getProjectRootDir();
        if (projectRootDir == null) {
            return null;
        }
        Collection<Path> localRepos = BazelRepoMappingProvider.getCanonicalRepoNameToPath(myProject).values();
        State newState = scan(Path.of(projectRootDir.getPath()), localRepos);
        if (this.myState.compareAndSet(null, newState)) {
            return newState;
        }

        return this.myState.get();
    }

    public State update(Path workspaceRoot, RepoMapping repoMapping) {
        State state = scan(workspaceRoot, RepoMapping.getLocalRepositories(repoMapping).localRepositories().values());
        this.myState.set(state);
        return state;
    }

    private State scan(Path workspaceRoot, Collection<Path> localRepos) {
        List<Path> errors = new ArrayList<>();
        Map<Path, List<BazelIgnoreMatcher>> patterns = new LinkedHashMap<>();
        patterns.put(workspaceRoot, scanRepoRoot(workspaceRoot, errors));
        for (Path localRepo : localRepos) {
            Path path = workspaceRoot.resolve(localRepo);
            patterns.put(path, scanRepoRoot(path, errors));
        }

        return new State(
            workspaceRoot,
            patterns,
            errors
        );
    }

    private List<BazelIgnoreMatcher> scanRepoRoot(Path path, List<Path> errors) {
        List<BazelIgnoreMatcher> matchers = new ArrayList<>();

        Path bazelIgnore = path.resolve(Constants.BAZEL_IGNORE_FILE_NAME);
        if (Files.exists(bazelIgnore)) {
            try {
                matchers.add(BazelIgnoreMatcher.BazelIgnoreMatcherFactory.fromBazelIgnoreFile(
                    Files.readString(bazelIgnore, StandardCharsets.UTF_8)
                ));
            }
            catch (Throwable ex) {
                rethrowControlFlowException(ex);
                errors.add(path);
                LOG.error("Error scanning file " + bazelIgnore, ex);
            }
        }

        Path repoBazel = path.resolve(Constants.REPO_BAZEL_FILE_NAME);
        if (Files.exists(repoBazel)) {
            try {
                matchers.add(BazelIgnoreMatcher.BazelIgnoreMatcherFactory.fromRepoBazelFile(
                    Files.readString(repoBazel, StandardCharsets.UTF_8)
                ));
            }
            catch (Throwable ex) {
                rethrowControlFlowException(ex);
                errors.add(path);
                LOG.error("Error scanning file " + repoBazel, ex);
            }
        }

        return matchers;
    }

    private static Map.@Nullable Entry<Path, List<BazelIgnoreMatcher>> matchesFor(State receiver, Path path) {
        Path p = path;
        while (p != null) {
            List<BazelIgnoreMatcher> machers = receiver.getPatterns().get(p);
            if (machers != null) {
                return Map.entry(p, machers);
            }
            p = p.getParent();
        }
        return null;
    }

    // consulo: Kotlin Path.relativeToOrNull()
    private static @Nullable Path relativeToOrNull(Path receiver, Path base) {
        try {
            return base.relativize(receiver);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    // consulo: IntelliJ rethrowControlFlowException()
    private static void rethrowControlFlowException(Throwable ex) {
        if (ex instanceof ControlFlowException || ex instanceof CancellationException) {
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (ex instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(ex);
        }
    }

    public static class State {
        private final Path myWorkspaceRoot;
        private final Map<Path, List<BazelIgnoreMatcher>> myPatterns;
        private final List<Path> myErrorsParsing;

        public State(
            Path workspaceRoot,
            Map<Path, List<BazelIgnoreMatcher>> patterns,
            List<Path> errorsParsing
        ) {
            myWorkspaceRoot = workspaceRoot;
            myPatterns = patterns;
            myErrorsParsing = errorsParsing;
        }

        public Path getWorkspaceRoot() {
            return myWorkspaceRoot;
        }

        public Map<Path, List<BazelIgnoreMatcher>> getPatterns() {
            return myPatterns;
        }

        public List<Path> getErrorsParsing() {
            return myErrorsParsing;
        }
    }
}

@ExtensionImpl
class UpdateBazelIgnoreSyncHook implements ProjectSyncHook {
    public UpdateBazelIgnoreSyncHook() {
    }

    @Override
    public CompletableFuture<Void> onSync(ProjectSyncHook.ProjectSyncHookEnvironment environment) {
        // consulo: upstream hard-coded (not localized) subtask text
        return environment.withSubtask(LocalizeValue.of("Update Bazel ignore files"), taskId -> {
            DefaultBazelIgnoreService service = BazelIgnoreService.getInstance(environment.project()) instanceof DefaultBazelIgnoreService it
                ? it
                : null;
            if (service != null) {
                DefaultBazelIgnoreService.State state = service.update(
                    environment.server().getBazelInfo().workspaceRoot(),
                    environment.snapshot().repoMapping()
                );
                for (Path path : state.getErrorsParsing()) {
                    util.getSyncConsole(environment.project()).addDiagnosticMessage(
                        taskId,
                        path, 0, 0,
                        LocalizeValue.of("Error parsing " + path.getFileName()),
                        null,
                        MessageEvent.Kind.ERROR
                    );
                }
            }
            return CompletableFuture.completedFuture(null);
        });
    }
}
