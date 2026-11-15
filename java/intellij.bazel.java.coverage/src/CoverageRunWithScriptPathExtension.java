package org.jetbrains.bazel.java.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.CoverageExecutor;
import consulo.execution.executor.Executor;
import org.jetbrains.bazel.jvm.run.RunWithScriptPathExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

/**
 * Run with {@code --script_path} if using Java coverage with an agent,
 * because we can add JVM arguments to the script without invalidating Bazel's analysis cache
 * and because adding a local Java agent isn't possible with remote execution
 */
@ExtensionImpl
public class CoverageRunWithScriptPathExtension implements RunWithScriptPathExtension {
    @Override
    public boolean shouldRunWithScriptPath(
        Executor executor,
        BazelRunConfiguration configuration
    ) {
        return executor instanceof CoverageExecutor && BazelJavaAgentCoverageEngine.isJavaAgentCoverageApplicableTo(configuration);
    }
}
