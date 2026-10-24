package org.jetbrains.bazel.server.bsp.utils;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

// consulo: holder class for the top-level declarations of upstream DelimitedMessageStream.kt
public final class DelimitedMessageStream {
    private DelimitedMessageStream() {
    }

    /**
     * Reads the bytes of one message. A varint size prefix comes first, and the message body follows it.
     * <p>
     * Throws {@link IllegalStateException} if the size prefix or the body does not arrive.
     * The prefix and the body each get their own {@code timeout}.
     */
    public static CompletableFuture<byte @Nullable []> awaitDelimitedMessageBytes(
        InputStream receiver,
        Duration timeout,
        Duration interval,
        BooleanSupplier stopCondition
    ) {
        // consulo: upstream default dispatcher is Dispatchers.IO
        return awaitDelimitedMessageBytes(
            receiver,
            timeout,
            interval,
            Application.get().getInstance(ApplicationConcurrency.class).executor(),
            stopCondition
        );
    }

    /**
     * Reads the bytes of one message. A varint size prefix comes first, and the message body follows it.
     * <p>
     * Throws {@link IllegalStateException} if the size prefix or the body does not arrive.
     * The prefix and the body each get their own {@code timeout}.
     */
    public static CompletableFuture<byte @Nullable []> awaitDelimitedMessageBytes(
        InputStream receiver,
        Duration timeout,
        Duration interval,
        Executor dispatcher,
        BooleanSupplier stopCondition
    ) {
        // consulo: upstream withContext(dispatcher); the blocking reads run on the executor, delay() is a sleep that
        // stops when the returned future is cancelled
        CompletableFuture<byte @Nullable []> job = new CompletableFuture<>();
        dispatcher.execute(() -> {
            try {
                if (!awaitAvailableBytesUnless(receiver, interval, stopCondition, job)) {
                    job.complete(null);
                    return;
                }
                int size = awaitMessageSize(receiver, timeout, interval, job);
                if (size < 0) {
                    throw new IllegalStateException("Invalid message size: " + size);
                }
                job.complete(awaitMessage(receiver, timeout, interval, size, job));
            }
            catch (IOException e) {
                job.completeExceptionally(new UncheckedIOException(e));
            }
            catch (Throwable e) {
                job.completeExceptionally(e);
            }
        });
        return job;
    }

    // caller must provide io dispatcher
    private static boolean awaitAvailableBytesUnless(
        InputStream receiver,
        Duration interval,
        BooleanSupplier stopCondition,
        CompletableFuture<?> job
    ) throws IOException {
        while (receiver.available() <= 0) {
            if (stopCondition.getAsBoolean()) {
                return false;
            }
            delay(interval, Long.MAX_VALUE, job);
        }
        return true;
    }

    // caller must provide io dispatcher
    private static byte[] awaitMessage(
        InputStream receiver,
        Duration timeout,
        Duration interval,
        int size,
        CompletableFuture<?> job
    ) throws IOException {
        byte[] messageArray = new byte[size];
        int length = 0;
        // consulo: upstream withTimeoutOrNull(timeout)
        long deadline = deadlineOf(timeout);
        boolean completed;
        try {
            while (length < size) {
                int readLength = receiver.readNBytes(messageArray, length, size - length);
                if (readLength == 0) {
                    delay(interval, deadline, job);
                }
                else {
                    length += readLength;
                }
            }
            completed = true;
        }
        catch (TimeoutCancellationException e) {
            completed = false;
        }
        if (!completed) {
            throw new IllegalStateException(
                "Incomplete message received, timed out waiting for the message body: Received " + length + " out of " + size + " bytes"
            );
        }
        return messageArray;
    }

    // caller must provide io dispatcher
    // Adapted from CodedInputStream.readRawVarint32
    private static int awaitMessageSize(
        InputStream receiver,
        Duration timeout,
        Duration interval,
        CompletableFuture<?> job
    ) throws IOException {
        // consulo: upstream withTimeoutOrNull(timeout), return@withTimeoutOrNull is a break out of the labeled block
        long deadline = deadlineOf(timeout);
        Integer size = null;
        try {
            withTimeout:
            {
                int firstByte = awaitByte(receiver, interval, deadline, job);
                if ((firstByte & 0x80) == 0) {
                    size = firstByte;
                    break withTimeout;
                }

                int result = firstByte & 0x7F;
                for (int offset = 7; offset < 32; offset += 7) {
                    int b = awaitByte(receiver, interval, deadline, job);
                    result = result | ((b & 0x7F) << offset);
                    if ((b & 0x80) == 0) {
                        size = result;
                        break withTimeout;
                    }
                }

                // A varint holds seven bits per byte, so a 64 bit value needs ten bytes. The code read five bytes above.
                // The bits of the last five bytes sit above bit 31, so they cannot change an Int. Read them to reach the body.
                for (int i = 0; i < 5; i++) {
                    if ((awaitByte(receiver, interval, deadline, job) & 0x80) == 0) {
                        size = result;
                        break withTimeout;
                    }
                }
                throw new IllegalStateException("Invalid message size format");
            }
        }
        catch (TimeoutCancellationException e) {
            size = null;
        }
        if (size == null) {
            throw new IllegalStateException("Incomplete message received, timed out waiting for the message size to be written");
        }
        return size;
    }

    // caller must provide io dispatcher
    private static int awaitByte(InputStream receiver, Duration interval, long deadline, CompletableFuture<?> job) throws IOException {
        while (true) {
            int b = receiver.read();
            if (b == -1) {
                delay(interval, deadline, job);
            }
            else {
                return b;
            }
        }
    }

    // consulo: deadline (System.nanoTime()) of upstream withTimeoutOrNull
    private static long deadlineOf(Duration timeout) {
        long now = System.nanoTime();
        long timeoutNanos = timeout.toNanos();
        return timeoutNanos >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeoutNanos;
    }

    /**
     * consulo: blocking counterpart of the kotlinx {@code delay(interval)}. Throws {@link TimeoutCancellationException} when the
     * deadline of the enclosing {@code withTimeoutOrNull} passes during the delay, and {@link CancellationException} when
     * {@code job} was cancelled or the thread was interrupted.
     */
    private static void delay(Duration interval, long deadline, CompletableFuture<?> job) {
        if (job.isDone()) {
            throw new CancellationException();
        }
        long intervalNanos = interval.toNanos();
        long remaining = deadline == Long.MAX_VALUE ? Long.MAX_VALUE : deadline - System.nanoTime();
        boolean timesOut = remaining < intervalNanos;
        try {
            long sleepNanos = Math.max(0, Math.min(intervalNanos, remaining));
            Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted");
        }
        if (timesOut) {
            throw new TimeoutCancellationException();
        }
        if (job.isDone()) {
            throw new CancellationException();
        }
    }

    // consulo: counterpart of the kotlinx TimeoutCancellationException caught by withTimeoutOrNull
    private static final class TimeoutCancellationException extends RuntimeException {
        private TimeoutCancellationException() {
            super(null, null, false, false);
        }
    }
}
