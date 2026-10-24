package org.jetbrains.bazel.bazelrunner.outputs;

import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

// consulo: upstream `typealias OutputHandler = (String) -> Unit` is java.util.function.Consumer<String>

public class OutputProcessor {
    private final Process myProcess;

    private final OutputCollector myStdoutCollector = new OutputCollector();
    private final OutputCollector myStderrCollector = new OutputCollector();

    private final ExecutorService myExecutorService = Executors.newCachedThreadPool();
    private final List<Future<?>> myRunningProcessors = new ArrayList<>();

    // consulo: overload for the upstream default arguments
    public OutputProcessor(Process process) {
        this(process, false, null);
    }

    public OutputProcessor(Process process, boolean showStdout, @Nullable Consumer<String> handler) {
        myProcess = process;

        @Nullable Consumer<String> stdoutHandler = showStdout ? handler : null;
        start(new CollectableInputStream(process.getInputStream(), myStdoutCollector), stdoutHandler);
        start(new CollectableInputStream(process.getErrorStream(), myStderrCollector), handler);
    }

    public OutputCollector getStdoutCollector() {
        return myStdoutCollector;
    }

    public OutputCollector getStderrCollector() {
        return myStderrCollector;
    }

    private void shutdown() {
        // Process is terminated here and stdout/stderr are closed already, Read buffered leftovers
        myRunningProcessors.forEach(it -> {
            try {
                it.get(1, TimeUnit.MINUTES); // Output handles should not be _that_ heavy after process is terminated
            }
            catch (InterruptedException | ExecutionException | TimeoutException e) {
                // consulo: Kotlin has no checked exceptions, upstream propagates them as is
                throw new IllegalStateException(e);
            }
        });
        myExecutorService.shutdown();
    }

    private void start(InputStream inputStream, @Nullable Consumer<String> handler) {
        Runnable runnable =
            () -> {
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        // Don't use readLine() because it doesn't preserve line separators
                        StringBuilder currentLine = new StringBuilder();
                        @Nullable Integer lookAheadChar = null;
                        while (!Thread.currentThread().isInterrupted()) {
                            int nextChar;
                            if (lookAheadChar != null) {
                                nextChar = lookAheadChar;
                                lookAheadChar = null;
                            }
                            else {
                                nextChar = reader.read();
                            }
                            if (nextChar == -1) {
                                if (currentLine.isEmpty()) {
                                    break;
                                }
                            }
                            else {
                                currentLine.append((char) nextChar);
                            }
                            // Don't split on \r if it's actually \r\n
                            if ((char) nextChar == '\r') {
                                // Only read the next char if it's already available, because PTY terminal can end a line with just \r to overwrite it.
                                // This .ready() check is probably not 100% reliable, worst case scenario we could send \n as a separate line.
                                if (reader.ready()) {
                                    nextChar = reader.read();
                                    if (nextChar == '\n') {
                                        currentLine.append((char) nextChar);
                                    }
                                    else {
                                        lookAheadChar = nextChar;
                                    }
                                }
                            }
                            String line;
                            if (!currentLine.isEmpty()
                                && (currentLine.charAt(currentLine.length() - 1) == '\n'
                                || currentLine.charAt(currentLine.length() - 1) == '\r'
                                || nextChar == -1)) {
                                line = currentLine.toString();
                                currentLine.setLength(0);
                            }
                            else {
                                continue;
                            }
                            if (handler != null) {
                                handler.accept(line);
                            }
                        }
                    }
                }
                catch (Throwable e) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    LOG.error("Error processing output", e);
                }
            };

        myRunningProcessors.add(myExecutorService.submit(runnable));
    }

    /**
     * consulo: upstream suspends in {@code process.awaitExit()}. Cancelling the returned future plays the role of the coroutine
     * cancellation: the process (tree) is killed and the cancellation is propagated. The upstream {@code finally} block
     * ({@link #shutdown()}) runs when the process has exited.
     */
    public CompletableFuture<Integer> waitForExit(boolean killProcessTreeOnCancel) {
        CompletableFuture<Integer> result = new CompletableFuture<>();
        myProcess.onExit().whenComplete((process, error) -> {
            try {
                shutdown();
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }
            if (error != null) {
                result.completeExceptionally(error);
            }
            else {
                result.complete(myProcess.exitValue());
            }
        });
        result.whenComplete((exitCode, error) -> {
            if (error instanceof CancellationException) {
                try {
                    if (killProcessTreeOnCancel) {
                        killProcessTree(myProcess);
                    }
                    else {
                        myProcess.destroyForcibly();
                    }
                }
                catch (Throwable ignored) {
                }
            }
        });
        return result;
    }

    // consulo: OSProcessUtil.killProcessTree has no public Consulo counterpart (OSProcessManager is internal),
    // the process tree is killed with java.lang.ProcessHandle
    private static void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static class CollectableInputStream extends InputStream {
        private final InputStream myDelegate;
        private final OutputCollector myCollector;

        CollectableInputStream(InputStream delegate, OutputCollector collector) {
            myDelegate = delegate;
            myCollector = collector;
        }

        @Override
        public void close() throws IOException {
            myDelegate.close();
        }

        @Override
        public int read() throws IOException {
            int it = myDelegate.read();
            if (it != -1) {
                myCollector.append(it);
            }
            return it;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int it = myDelegate.read(b, off, len);
            if (it > 0) {
                myCollector.append(b, off, it);
            }
            return it;
        }

        @Override
        public int available() throws IOException {
            return myDelegate.available();
        }
    }


    private static final Logger LOG = Logger.getInstance(OutputProcessor.class);
}
