package org.jetbrains.bazel.bazelrunner;

import org.jetbrains.bazel.bazelrunner.outputs.OutputProcessor;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.util.Format;
import org.jetbrains.bazel.util.Stopwatch;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class BazelProcess {
    private final Process myProcess;
    private final boolean myShowStdout;
    private final @Nullable BazelTaskLogger myLogger;
    private final Runnable myFinishCallback;

    // consulo: overload for the upstream default arguments; the constructors are `internal` upstream
    public BazelProcess(Process process, boolean showStdout) {
        this(process, showStdout, null, () -> {
        });
    }

    public BazelProcess(
        Process process,
        boolean showStdout,
        @Nullable BazelTaskLogger logger,
        Runnable finishCallback
    ) {
        myProcess = process;
        myShowStdout = showStdout;
        myLogger = logger;
        myFinishCallback = finishCallback;
    }

    public long getPid() {
        return myProcess.pid();
    }

    /**
     * consulo: upstream suspend function. Cancelling the returned future cancels the wait for the process exit
     * (see {@link OutputProcessor#waitForExit(boolean)}).
     */
    public CompletableFuture<BazelProcessResult> waitAndGetResult() {
        CompletableFuture<Integer> exitCodeFuture;
        CompletableFuture<BazelProcessResult> processResult;
        try {
            Stopwatch stopwatch = Stopwatch.start();
            @Nullable BazelTaskLogger logger = myLogger;
            OutputProcessor outputProcessor =
                new OutputProcessor(
                    myProcess,
                    myShowStdout,
                    logger != null ? logger::message : null
                );

            exitCodeFuture = outputProcessor.waitForExit(BazelFeatureFlags.getKillClientTreeOnCancel());
            processResult = exitCodeFuture.thenApply(exitCode -> {
                Duration duration = stopwatch.stop();
                if (logger != null) {
                    logger.info("Command completed in " + Format.duration(duration) + " (exit code " + exitCode + ")");
                }

                BazelLog.write(writer -> {
                    writer.append("exit code: ").append(String.valueOf(exitCode)).append('\n');
                    writer.append("stdout:\n")
                        .append(new String(outputProcessor.getStdoutCollector().raw(), StandardCharsets.UTF_8))
                        .append('\n');

                    List<String> stdErrLines = dropLastWhileBlank(dropWhileBlank(outputProcessor.getStderrCollector().lines()));
                    if (!stdErrLines.isEmpty()) {
                        writer.append("stderr:").append('\n');
                        stdErrLines.forEach(it -> {
                            writer.append(it).append('\n');
                        });
                    }
                });

                return new BazelProcessResult(outputProcessor.getStdoutCollector(), outputProcessor.getStderrCollector(), exitCode);
            });
        }
        catch (Throwable e) {
            myFinishCallback.run();
            return CompletableFuture.failedFuture(e);
        }
        // consulo: the upstream `finally` block runs when the future completes
        CompletableFuture<BazelProcessResult> result = processResult.whenComplete((value, error) -> myFinishCallback.run());
        // consulo: cancellation of the result is propagated to the process wait, like the coroutine cancellation
        result.whenComplete((value, error) -> {
            if (error instanceof CancellationException) {
                exitCodeFuture.cancel(false);
            }
        });
        return result;
    }

    public void destroy() {
        myProcess.destroy();
    }

    // consulo: Kotlin List.dropWhile { it.isBlank() }
    private static List<String> dropWhileBlank(List<String> lines) {
        int start = 0;
        while (start < lines.size() && lines.get(start).isBlank()) {
            start++;
        }
        return lines.subList(start, lines.size());
    }

    // consulo: Kotlin List.dropLastWhile { it.isBlank() }
    private static List<String> dropLastWhileBlank(List<String> lines) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).isBlank()) {
            end--;
        }
        return lines.subList(0, end);
    }
}
