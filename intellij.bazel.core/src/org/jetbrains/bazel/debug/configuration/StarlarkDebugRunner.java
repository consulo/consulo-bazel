package org.jetbrains.bazel.debug.configuration;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.Task;
import consulo.bazel.commons.BazelRegistryKeys;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.runner.AsyncProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.io.NetUtil;
import org.jdom.Element;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.debug.connector.StarlarkDebugSessionManager;
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector;
import org.jetbrains.bazel.debug.console.StarlarkDebugTaskListener;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugParams;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugResult;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

@ExtensionImpl
public class StarlarkDebugRunner extends AsyncProgramRunner<StarlarkDebugRunner.Settings> {
    @Override
    public String getRunnerId() {
        return "StarlarkDebugRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        return executorId.equals(DefaultDebugExecutor.EXECUTOR_ID) &&
            profile instanceof StarlarkDebugConfiguration;
    }

    // consulo: upstream overrides execute(environment, state) returning a Promise; Consulo's AsyncProgramRunner.executeImpl
    // returns a CompletableFuture
    @Override
    protected CompletableFuture<RunContentDescriptor> executeImpl(RunProfileState state, ExecutionEnvironment environment) {
        CompletableFuture<RunContentDescriptor> promise = new CompletableFuture<>();
        BazelCoroutineService.getInstance(environment.getProject()).start(() ->
            executeAndCompletePromise(environment, state, promise)
        );
        return promise;
    }

    private CompletableFuture<?> executeAndCompletePromise(
        ExecutionEnvironment environment,
        RunProfileState state,
        CompletableFuture<RunContentDescriptor> promise
    ) {
        // cast will always succeed - canRun(...) makes sure only StarlarkDebugConfiguration is run with this runner
        StarlarkDebugConfigurationState starlarkState = (StarlarkDebugConfigurationState) state;
        Project project = environment.getProject();
        int port = choosePort();
        StarlarkDebugSessionManager starlarkManager = new StarlarkDebugSessionManager(project);
        Label target = Label.parse(starlarkState.getTarget());

        return connectAndExecuteAnalysisDebug(project, port, starlarkManager.getTaskListener(), target, starlarkState.getFutureProxy())
            .thenCompose(debugJob -> {
                debugJob.whenComplete((result, error) -> Disposer.dispose(starlarkManager));
                // Function connectAndExecuteAnalysisDebug(...) will not return until a server connection is established.
                //   Thanks to that, we are not trying to connect to the debug server while an auto-connect is still in progress.
                return connectToDebugServer(project, port)
                    .thenCompose(connector -> {
                        StarlarkDebugSessionManager.ProcessStarter starter = starlarkManager.new ProcessStarter(connector);
                        starlarkManager.registerJobToCancel(debugJob);
                        // consulo: withContext(Dispatchers.EDT) -> UIAccess.giveAsync
                        return project.getUIAccess().giveAsync(() -> {
                            try {
                                // consulo: XDebuggerManager.newSessionBuilder(starter).environment(environment).startSession()
                                //   -> startSession(environment, starter)
                                return XDebuggerManager.getInstance(project).startSession(environment, starter).getRunContentDescriptor();
                            }
                            catch (ExecutionException e) {
                                throw new CompletionException(e);
                            }
                        });
                    })
                    .handle((descriptor, error) -> {
                        if (error == null) {
                            promise.complete(descriptor);
                            return null;
                        }
                        Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                        if (e instanceof CancellationException) {
                            // user cancelled the connection attempt before it was completed
                            debugJob.cancel(true);
                            promise.completeExceptionally(new ExecutionException(BazelPluginLocalize.starlarkDebugExceptionCancellation()));
                        }
                        else if (e instanceof ConnectException) {
                            // Socket failed to connect - Bazel server might be stuck waiting for debug connection.
                            //   In that situation debugJob.cancel() will not stop the server, as we don't have the server PID yet.
                            debugJob.cancel(true);
                            promise.completeExceptionally(new ExecutionException(BazelPluginLocalize.starlarkDebugExceptionConnect()));
                        }
                        else {
                            // unexpected error, we are not prepared for it - log it
                            debugJob.cancel(true);
                            promise.completeExceptionally(e);
                            LOG.error(e);
                        }
                        return null;
                    });
            });
    }

    private CompletableFuture<CompletableFuture<?>> connectAndExecuteAnalysisDebug(
        Project project,
        int port,
        StarlarkDebugTaskListener taskListener,
        Label target,
        CompletableFuture<Integer> futureProxy
    ) {
        return BazelServerConnection.getConnection(project).runWithServer(server ->
            CompletableFuture.<CompletableFuture<?>>completedFuture(
                BazelCoroutineService.getInstance(project).start(() ->
                    analysisDebug(project, port, taskListener, target, server, futureProxy)
                )
            )
        );
    }

    private CompletableFuture<?> analysisDebug(
        Project project,
        int port,
        StarlarkDebugTaskListener taskListener,
        Label target,
        BazelServerFacade server,
        CompletableFuture<Integer> futureProxy
    ) {
        byte[] randomBytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        TaskGroupId taskGroupId = new TaskGroupId("analysis-debug-" + HexFormat.of().formatHex(randomBytes));
        BazelTaskEventsService.getInstance(project).saveListener(taskGroupId, taskListener);
        AnalysisDebugParams params = new AnalysisDebugParams(taskGroupId.task("debug"), port, List.of(target));

        CompletableFuture<AnalysisDebugResult> buildDeferred = server.buildTargetAnalysisDebug(params);
        return buildDeferred.handle((result, e) -> {
            try {
                if (e == null) {
                    futureProxy.complete(result.exitCode());
                }
                else {
                    buildDeferred.cancel(true);
                    futureProxy.completeExceptionally(e instanceof CompletionException && e.getCause() != null ? e.getCause() : e);
                }
            }
            finally {
                BazelTaskEventsService.getInstance(project).removeListener(taskGroupId);
            }
            return null;
        });
    }

    // consulo: throws ConnectException, CancellationException (through the returned future)
    private CompletableFuture<StarlarkSocketConnector> connectToDebugServer(Project project, int port) {
        // consulo: withBackgroundProgress -> Task.Backgroundable.queue
        CompletableFuture<StarlarkSocketConnector> result = new CompletableFuture<>();
        Task.Backgroundable.queue(project, BazelPluginLocalize.starlarkDebugConnectingProgressName(), true, indicator -> {
            try {
                result.complete(
                    StarlarkSocketConnector.tryConnectTo(
                        port,
                        BazelRegistryKeys.intValue("bazel.starlark.debug.socket.attempts"),
                        (long) BazelRegistryKeys.intValue("bazel.starlark.debug.socket.interval")
                    ).join()
                );
            }
            catch (ProcessCanceledException e) {
                result.completeExceptionally(new CancellationException());
            }
            catch (CompletionException e) {
                result.completeExceptionally(e.getCause() != null ? e.getCause() : e);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    public static class Settings implements RunnerSettings {
        @Override
        public void readExternal(Element element) {
            // empty settings, don't do anything
        }

        @Override
        public void writeExternal(Element element) {
            // empty settings, don't do anything
        }
    }

    private static final Logger LOG = Logger.getInstance(StarlarkDebugRunner.class);

    private static int choosePort() {
        try {
            return NetUtil.findAvailableSocketPort();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
