package com.intellij.bazel.java.profiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.executor.Executor;
import consulo.execution.profiler.DefaultProfilerExecutorGroup;
import consulo.execution.profiler.ProfilerExecutorSettings;
import org.jetbrains.bazel.jvm.run.RunWithScriptPathExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

/**
 * Always run with {@code --script_path} for Profiler,
 * because it makes sure the process is run locally (unlike with {@code bazel run} and remote execution),
 * and because we can add JVM arguments to the script without invalidating Bazel's analysis cache
 */
@ExtensionImpl
public class ProfilerRunWithScriptPathExtension implements RunWithScriptPathExtension {
    public ProfilerRunWithScriptPathExtension() {
    }

    @Override
    public boolean shouldRunWithScriptPath(
        Executor executor,
        BazelRunConfiguration configuration
    ) {
        return isProfilerExecutor(executor.getId());
    }

    private boolean isProfilerExecutor(String executorId) {
        DefaultProfilerExecutorGroup group = DefaultProfilerExecutorGroup.getInstance();
        ProfilerExecutorSettings settings = group == null ? null : group.getRegisteredSettings(executorId);
        // consulo: ProfilerExecutorSettings.getState() is never null, upstream checked `?.state != null`
        return settings != null;
    }
}
