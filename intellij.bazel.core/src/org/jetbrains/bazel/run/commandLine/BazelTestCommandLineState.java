package org.jetbrains.bazel.run.commandLine;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.ExecutionResult;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.AbstractGenericTestState;
import org.jetbrains.bazel.run.task.BazelTestTaskListener;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TestParams;
import org.jetbrains.bsp.protocol.TestResult;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BazelTestCommandLineState extends BazelCommandLineStateBase {
    private static final String COVERAGE_EXECUTOR_ID = "Coverage";

    private final AbstractGenericTestState<?> myState;
    private @Nullable Consumer<Path> myCoverageReportListener = null;

    private final BazelRunConfiguration myConfiguration;

    public BazelTestCommandLineState(ExecutionEnvironment environment, AbstractGenericTestState<?> state) {
        super(environment);
        myState = state;
        myConfiguration = BazelRunConfiguration.get(environment);
    }

    public AbstractGenericTestState<?> getState() {
        return myState;
    }

    public @Nullable Consumer<Path> getCoverageReportListener() {
        return myCoverageReportListener;
    }

    public void setCoverageReportListener(@Nullable Consumer<Path> coverageReportListener) {
        myCoverageReportListener = coverageReportListener;
    }

    protected TestParams transformTestParams(TestParams params) {
        return params;
    }

    @Override
    public ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
        return executeWithTestConsole(executor);
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return new BazelTestTaskListener(handler, myCoverageReportListener);
    }

    @Override
    protected CompletableFuture<Integer> runCommand(
        BazelServerFacade server,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler
    ) {
        if (myConfiguration.getTargets().isEmpty()) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelPluginLocalize.bspRunErrorCannotrun()));
        }

        // TODO: add pidDeferred to TestParams
        TestParams params =
            new TestParams(
                /* taskId = */ getTaskGroupId().task("test"),
                /* targets = */ myConfiguration.getTargets(),
                /* arguments = */ Utils.parseAsProgramArguments(myState.getProgramArguments()),
                /* environmentVariables = */ myState.getEnv().getEnvs(),
                /* useCoverage = */ getEnvironment().getExecutor().getId().equals(COVERAGE_EXECUTOR_ID),
                /* coverageInstrumentationFilter = */ myState.getCoverageInstrumentationFilter(),
                /* testFilter = */ myState.getTestFilter(),
                /* additionalBazelParams = */ Utils.parseAsProgramArguments(myState.getAdditionalBazelParams()),
                /* streamTestOutput = */ false
            );
        return server.buildTargetTest(transformTestParams(params)).thenApply(TestResult::exitCode);
    }
}
