package org.jetbrains.bazel.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionImpl;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.dumb.DumbAware;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jetbrains.bazel.config.project.isBazelProject;

/**
 * Fetches the environment of a login shell to avoid cases where `PATH` is different in IDEA and in terminal (BAZEL-883).
 * {@code com.intellij.util.EnvironmentUtil.getEnvironmentMap} already does this,
 * but only on macOS and only if IDEA wasn't launched from terminal or as a dev build (see com.intellij.platform.ide.bootstrap.StartupUtil#shouldLoadShellEnv).
 * For Bazel plugin we want to skip these conditions and always run Bazel as if it ran in the terminal.
 *
 * @see "com.intellij.util.ShellEnvironmentReader"
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelEnvironmentService {
    private static final Logger LOG = Logger.getInstance(BazelEnvironmentService.class);

    private final CompletableFuture<Map<String, String>> myEnvironmentDeferred;

    @Inject
    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public BazelEnvironmentService(Project project) {
        CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
        myEnvironmentDeferred = Coroutine.first(CodeExecution.<Object, Map<String, String>>supply(() -> {
                // Using interactive login shell here to make sure the environment variables match the user's terminal exactly.
                // Non-interactive shells miss environment variables defined in files like ~/.bashrc or ~/.zshrc
                return Platform.current().os().environmentVariables();
            }))
            .runAsync(scope, null)
            .toFuture()
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(e -> {
                LOG.warn(e);
                return System.getenv();
            });
    }

    public CompletableFuture<Map<String, String>> getEnvironment() {
        return myEnvironmentDeferred;
    }

    public CompletableFuture<@Nullable Path> findInPath(String... executableNames) {
        return getEnvironment().thenApply(environment -> {
            String pathValue = environment.get("PATH");
            if (pathValue == null) {
                return null;
            }
            return findExecutableInPath(pathValue, executableNames);
        });
    }

    public static BazelEnvironmentService getInstance(Project project) {
        return project.getInstance(BazelEnvironmentService.class);
    }

    /**
     * Returns the first executable regular file with one of {@code executableNames} in the directories of {@code pathValue}.
     * The directory order comes first, as in a shell: the first directory with any of the names wins.
     * In one directory, the order of {@code executableNames} decides.
     * The search skips a directory with a matching name, and an empty or a relative entry of {@code pathValue}.
     * On Windows, the search accepts only {@code <name>.exe}.
     * A Windows process cannot run a file without an extension, and every file on Windows is executable.
     */
    // consulo: upstream top-level function of BazelEnvironmentService.kt
    public static @Nullable Path findExecutableInPath(String pathValue, String... executableNames) {
        List<String> fileNames = Platform.current().os().isWindows()
            ? Stream.of(executableNames).map(it -> it + ".exe").toList()
            : List.of(executableNames);
        for (Path dir : getPathDirs(pathValue)) {
            for (String name : fileNames) {
                Path file = findFirst(name, dir);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }

    // consulo: IntelliJ PathEnvironmentVariableUtil.getPathDirs(pathValue), missing in Consulo PathEnvironmentVariableUtil
    private static List<Path> getPathDirs(String pathValue) {
        List<Path> result = new ArrayList<>();
        for (String entry : pathValue.split(Pattern.quote(File.pathSeparator))) {
            if (entry.isEmpty()) {
                continue;
            }
            Path dir;
            try {
                dir = Path.of(entry);
            }
            catch (InvalidPathException e) {
                continue;
            }
            if (dir.isAbsolute()) {
                result.add(dir);
            }
        }
        return result;
    }

    // consulo: IntelliJ PathEnvironmentVariableUtil.findFirst(name, dir), missing in Consulo PathEnvironmentVariableUtil
    private static @Nullable Path findFirst(String name, Path dir) {
        Path file = dir.resolve(name);
        return Files.isRegularFile(file) && Files.isExecutable(file) ? file : null;
    }
}

// consulo: upstream nests this class in BazelEnvironmentService; Consulo extensions must be top-level
@ExtensionImpl
class StartupActivity implements PostStartupActivity, DumbAware {
    public StartupActivity() {
    }

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (isBazelProject(project)) {
            BazelEnvironmentService.getInstance(project);  // Make sure environmentDeferred starts its calculation ASAP
        }
    }
}
