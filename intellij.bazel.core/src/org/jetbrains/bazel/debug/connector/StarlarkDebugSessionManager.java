package org.jetbrains.bazel.debug.connector;

import consulo.application.progress.Task;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugProcessStarter;
import consulo.execution.debug.XDebugSession;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.debug.console.StarlarkDebugTaskListener;
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler;
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Main controller for everything related to Starlark debug.
 * All debug elements which require disposal should have this class registered as their disposable parent.
 * Must be disposed after use.
 */
public class StarlarkDebugSessionManager implements Disposable {
    private final Project myProject;

    private final StarlarkDebugTaskListener myTaskListener;

    private final ConsoleView myConsole;

    private @Nullable CompletableFuture<?> myJobToCancel = null; // job to be cancelled when debug stops
    private @Nullable StarlarkDebugMessenger myMessenger = null;
    private @Nullable ThreadAwareEventHandler myEventHandler = null;

    private volatile @Nullable CompletableFuture<?> myLoop = null;

    public StarlarkDebugSessionManager(Project project) {
        myProject = project;
        myConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Disposer.register(this, myConsole);
        myTaskListener = new StarlarkDebugTaskListener(myConsole);
    }

    public StarlarkDebugTaskListener getTaskListener() {
        return myTaskListener;
    }

    private StarlarkDebugProcess startDebuggingProcess(XDebugSession session, StarlarkSocketConnector connector) {
        StarlarkDebugMessenger it = new StarlarkDebugMessenger(connector, session);
        myMessenger = it;
        StarlarkBreakpointHandler breakpointHandler = new StarlarkBreakpointHandler(it);
        myEventHandler = new ThreadAwareEventHandler(session, it, breakpointHandler::getBreakpointByPathAndLine);
        myLoop = BazelCoroutineService.getInstance(myProject).start(this::runLoop);
        StarlarkDebugProcess debugProcess =
            new StarlarkDebugProcess(connector, session, breakpointHandler, myConsole);
        myTaskListener.setDebugPausedChecker(debugProcess::isSuspended);
        return debugProcess;
    }

    // consulo: withBackgroundProgress -> Task.Backgroundable.queue; the coroutine isActive check -> the progress indicator
    //   is not cancelled and the loop job is not cancelled
    private CompletableFuture<?> runLoop() {
        CompletableFuture<@Nullable Object> result = new CompletableFuture<>();
        Task.Backgroundable.queue(myProject, BazelPluginLocalize.starlarkDebugTaskTitle(), true, indicator -> {
            try {
                while (!indicator.isCanceled() && isLoopActive()) {
                    performEventLoopIteration();
                }
                result.complete(null);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    // consulo: Java counterpart of the coroutine isActive check of runLoop()
    private boolean isLoopActive() {
        CompletableFuture<?> loop = myLoop;
        return loop == null || !loop.isDone();
    }

    private void performEventLoopIteration() {
        try {
            ThreadAwareEventHandler it = myEventHandler;
            if (it != null) {
                StarlarkDebugMessenger messenger = myMessenger;
                if (messenger != null) {
                    messenger.readEventAndHandle(it);
                }
            }
        }
        catch (Exception e) {
            if (!(e instanceof IOException) && !(e instanceof CancellationException)) {
                LOG.error(e); // other exceptions are not expected
            }
            stop();
        }
    }

    /**
     * This function needs to be safe to call multiple times
     */
    private void stop() {
        CompletableFuture<?> loop = myLoop;
        if (loop != null) {
            loop.cancel(true);
        }
        CompletableFuture<?> jobToCancel = myJobToCancel;
        if (jobToCancel != null) {
            jobToCancel.cancel(true);
        }
        StarlarkDebugMessenger messenger = myMessenger;
        if (messenger != null) {
            messenger.close();
        }
    }

    public void registerJobToCancel(CompletableFuture<?> job) {
        // if for some reason, another job is registered, cancel the previous one
        CompletableFuture<?> jobToCancel = myJobToCancel;
        if (jobToCancel != null) {
            jobToCancel.cancel(true);
        }
        myJobToCancel = job;
    }

    /**
     * Gets a single debug session manager loop iteration for testing it without performing full session setup
     */
    @TestOnly
    public Runnable getSingleLoopIteration(StarlarkDebugMessenger messenger, ThreadAwareEventHandler eventHandler) {
        this.myMessenger = messenger;
        this.myEventHandler = eventHandler;
        return this::performEventLoopIteration;
    }

    @Override
    public void dispose() {
        // no action - manager does not dispose anything itself, it just serves as parent for other disposables
    }

    public class ProcessStarter implements XDebugProcessStarter {
        private final StarlarkSocketConnector myConnector;

        public ProcessStarter(StarlarkSocketConnector connector) {
            myConnector = connector;
        }

        @Override
        public XDebugProcess start(XDebugSession session) {
            return startDebuggingProcess(session, myConnector);
        }
    }

    private static final Logger LOG = Logger.getInstance(StarlarkDebugSessionManager.class);
}
