package org.jetbrains.bazel.run;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.process.BaseProcessHandler;
import consulo.process.ProcessOutputType;
import consulo.project.Project;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BazelProcessHandler extends BaseProcessHandler {
    private final Project myProject;
    private final CompletableFuture<Integer> myRunDeferred;
    private final @Nullable CompletableFuture<@Nullable Long> myPid;

    public BazelProcessHandler(Project project, CompletableFuture<Integer> runDeferred, @Nullable CompletableFuture<@Nullable Long> pid) {
        myProject = project;
        myRunDeferred = runDeferred;
        myPid = pid;
    }

    public BazelProcessHandler(Project project, CompletableFuture<Integer> runDeferred) {
        this(project, runDeferred, null);
    }

    public @Nullable CompletableFuture<@Nullable Long> getPid() {
        return myPid;
    }

    @Override
    public void startNotify() {
        super.startNotify();
        myRunDeferred.whenComplete((result, error) -> {
            // invokeOnCompletion is called synchronously inside a canceled coroutine, we don't want that
            BazelCoroutineService.getInstance(myProject).start(() -> {
                // consulo: dependent stages may see the failure wrapped into a CompletionException
                @Nullable Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                if (e == null) {
                    notifyProcessTerminated(result);
                }
                else if (e instanceof CancellationException) {
                    notifyTextAvailable(BazelPluginLocalize.consoleTaskRunCancelled().get(), ProcessOutputType.STDERR);
                    notifyProcessTerminated(1);
                }
                else {
                    notifyTextAvailable(e.toString(), ProcessOutputType.STDERR);
                    notifyProcessTerminated(1);
                }
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    @Override
    protected void destroyProcessImpl() {
        myRunDeferred.cancel(true);
    }

    @Override
    protected void detachProcessImpl() {
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return null;
    }

    // consulo: Consulo's ProcessHandler has no getNativePid(), kept for upstream callers
    public final @Nullable CompletableFuture<@Nullable Long> getNativePid() {
        return myPid;
    }
}
