package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.RunWithBazel;

import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunWithScriptPathExtension {
    /**
     * Return {@code true} if {@code bazel run --script_path=foo //foo && ./foo} should be used instead of calling Bazel directly
     * for this {@code executor} and {@code configuration}.
     *
     * @see org.jetbrains.bazel.jvm.run.ScriptPathBeforeRunTaskProvider
     */
    boolean shouldRunWithScriptPath(Executor executor, BazelRunConfiguration configuration);

    // consulo: upstream companion object; a static interface method cannot have the signature of the abstract method above, so the
    // companion is kept as a nested class: RunWithScriptPathExtension.Companion.shouldRunWithScriptPath(executor, configuration)
    final class Companion {
        private Companion() {
        }

        public static boolean shouldRunWithScriptPath(Executor executor, BazelRunConfiguration configuration) {
            // Because `bazel run` only supports one target, so does `bazel run --script_path`
            List<Label> targets = configuration.getTargets();
            if (targets.size() != 1) {
                return false;
            }
            for (Label it : targets) {
                if (it.target() instanceof Label.AllRuleTargets) {
                    return false;
                }
            }
            for (RunWithScriptPathExtension it : getEp()) {
                if (it.shouldRunWithScriptPath(executor, configuration)) {
                    return true;
                }
            }
            return false;
        }

        private static List<RunWithScriptPathExtension> getEp() {
            return Application.get().getExtensionList(RunWithScriptPathExtension.class);
        }
    }
}

/**
 * If {@link RunWithBazel.HasRunWithBazel#getRunWithBazel()} was set to {@code false} by the user, use --script_path instead of
 * {@code bazel run/test} with {@link DefaultRunExecutor}
 */
@ExtensionImpl
class DefaultRunWithScriptPathExtension implements RunWithScriptPathExtension {
    public DefaultRunWithScriptPathExtension() {
    }

    @Override
    public boolean shouldRunWithScriptPath(
        Executor executor,
        BazelRunConfiguration configuration
    ) {
        BazelRunHandler handler = configuration.getHandler();
        return executor instanceof DefaultRunExecutor
            && handler != null
            && handler.getState() instanceof RunWithBazel.HasRunWithBazel hasRunWithBazel
            && !hasRunWithBazel.getRunWithBazel();
    }
}

/**
 * Always run with --script_path when debugging, because:
 * - With remote execution, our Java debug port will be open on the remote server instead of locally
 * - {@code --script_path} doesn't hold Bazel's lock, allowing hotswap together with debug.
 * This is impossible with plain {@code bazel run} as you can't run {@code bazel build} in parallel.
 * - Calling {@code bazel run} may involve compilation, causing debugger attachment to time out.
 */
@ExtensionImpl
class DebugRunWithScriptPathExtension implements RunWithScriptPathExtension {
    public DebugRunWithScriptPathExtension() {
    }

    @Override
    public boolean shouldRunWithScriptPath(
        Executor executor,
        BazelRunConfiguration configuration
    ) {
        return executor instanceof DefaultDebugExecutor;
    }
}
