package org.jetbrains.bazel.run;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.action.AbstractRerunFailedTestsAction;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.ui.SMTRunnerConsoleView;
import consulo.execution.ui.RunContentManager;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;
import org.jspecify.annotations.Nullable;

import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BazelCommandLineStateBase extends CommandLineState {
    private final TaskGroupId myTaskGroupId;

    protected BazelCommandLineStateBase(ExecutionEnvironment environment) {
        super(environment);
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        myTaskGroupId = new TaskGroupId(environment.toString() + "-" + HexFormat.of().formatHex(randomBytes));
    }

    protected TaskGroupId getTaskGroupId() {
        return myTaskGroupId;
    }

    protected boolean isIdBasedTestTree() {
        return false;
    }

    protected abstract BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler);

    /**
     * Run the command and return its exit code, or throw an exception if the server does not support running the configuration
     */
    protected abstract CompletableFuture<Integer> runCommand(
        BazelServerFacade server,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler
    );

    @Override
    protected final BazelProcessHandler startProcess() {
        return doStartProcess(false);
    }

    private BazelProcessHandler doStartProcess(boolean runningTests) {
        BazelRunConfiguration configuration = BazelRunConfiguration.get(getEnvironment());
        Project project = configuration.getProject();

        BazelCoroutineService bazelCoroutineService = BazelCoroutineService.getInstance(project);

        // consulo: upstream `lateinit var handler`
        SimpleReference<BazelProcessHandler> handler = new SimpleReference<>();
        // We have to start runDeferred later, because we need to register the listener first
        // Otherwise, we might miss some events

        CompletableFuture<@Nullable Long> pid = new CompletableFuture<>();
        BazelCoroutineService.Deferred<Integer> runDeferred =
            bazelCoroutineService.startAsync(true, () ->
                BazelServerConnection.getConnection(project).runWithServer((BazelServerFacade server) ->
                    SaveAllFiles.saveAllFiles()
                        .thenCompose(ignored -> project.getUIAccess().giveAsync(() ->
                            // consulo: RunContentManager has no static getInstance(project)
                            project.getInstance(RunContentManager.class)
                                .toFrontRunContent(getEnvironment().getExecutor(), Objects.requireNonNull(handler.get()))
                        ))
                        .thenCompose(ignored -> runCommand(server, pid, Objects.requireNonNull(handler.get())))
                )
            );

        BazelProcessHandler processHandler = runningTests
            ? new BazelTestProcessHandler(project, runDeferred, pid, isIdBasedTestTree())
            : new BazelProcessHandler(project, runDeferred, pid);
        handler.set(processHandler);

        BazelTaskListener runListener = createAndAddTaskListener(processHandler);

        BazelTaskEventsService taskEventsService = BazelTaskEventsService.getInstance(project);
        taskEventsService.saveListener(myTaskGroupId, runListener);
        runDeferred.whenComplete((result, error) -> {
            pid.complete(null);
            taskEventsService.removeListener(myTaskGroupId);
        });

        runDeferred.start();

        return processHandler;
    }

    protected ExecutionResult executeWithTestConsole(Executor executor) {
        BazelRunConfiguration configuration = BazelRunConfiguration.get(getEnvironment());
        SMTRunnerConsoleProperties properties = configuration.createTestConsoleProperties(executor);
        properties.setIdBasedTestTree(isIdBasedTestTree());
        BazelProcessHandler handler = doStartProcess(true);

        // consulo: SMTestRunnerConnectionUtil.createConsole(properties) takes the framework name explicitly and always creates
        // an SMTRunnerConsoleView
        SMTRunnerConsoleView console =
            (SMTRunnerConsoleView) SMTestRunnerConnectionUtil.createConsole(
                properties.getTestFrameworkName(),
                properties
            );
        console.attachToProcess(handler);
        DefaultExecutionResult executionResult = new DefaultExecutionResult(console, handler);
        AbstractRerunFailedTestsAction rerunFailedTestsAction = properties.createRerunFailedTestsAction(console);
        if (rerunFailedTestsAction != null) {
            executionResult.setRestartActions(rerunFailedTestsAction);
        }
        return executionResult;
    }
}
