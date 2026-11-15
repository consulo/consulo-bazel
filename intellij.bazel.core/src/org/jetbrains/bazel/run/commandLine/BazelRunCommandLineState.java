package org.jetbrains.bazel.run.commandLine;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.AbstractGenericRunState;
import org.jetbrains.bazel.run.task.BazelRunTaskListener;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.RunParams;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class BazelRunCommandLineState extends BazelCommandLineStateBase {
    private final AbstractGenericRunState<?> myRunState;
    private final BazelRunConfiguration myConfiguration;

    public BazelRunCommandLineState(ExecutionEnvironment environment, AbstractGenericRunState<?> runState) {
        super(environment);
        myRunState = runState;
        myConfiguration = BazelRunConfiguration.get(environment);
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return new BazelRunTaskListener(handler);
    }

    @Override
    protected CompletableFuture<?> startBsp(
        BazelServerFacade server,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler
    ) {
        if (myConfiguration.getTargets().size() != 1) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelPluginLocalize.bspRunErrorCannotrun()));
        }

        RunParams runParams =
            new RunParams(
                /* taskId = */ getTaskGroupId().task("run"),
                /* target = */ myConfiguration.getTargets().get(0),
                /* checkVisibility = */ myConfiguration.getDoVisibilityCheck(),
                /* arguments = */ Utils.parseAsProgramArguments(myRunState.getProgramArguments()),
                /* environmentVariables = */ myRunState.getEnv().getEnvs(),
                /* additionalBazelParams = */ Utils.parseAsProgramArguments(myRunState.getAdditionalBazelParams()),
                /* pidDeferred = */ pidDeferred,
                /* captureStdout = */ false
            );
        return server.buildTargetRun(runParams);
    }
}
