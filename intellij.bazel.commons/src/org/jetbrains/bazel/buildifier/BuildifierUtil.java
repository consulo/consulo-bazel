package org.jetbrains.bazel.buildifier;

import consulo.project.Project;
import org.jetbrains.bazel.sync.BazelEnvironmentService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class BuildifierUtil {
    private BuildifierUtil() {
    }

    public static @Nullable String detectBuildifierExecutable(Project project) {
        // runBlockingCancellable is not allowed inside a write action, which causes Starlark formatting to throw exceptions
        // Also, BazelEnvironmentService starts the calculation on startup (see BazelEnvironmentService$StartupActivity),
        // so by the time this function is called it should've been finished already
        // consulo: upstream runBlocking { }
        Path path = BazelEnvironmentService.getInstance(project).findInPath("buildifier").join();
        return path != null ? path.toAbsolutePath().toString() : null;
    }
}
