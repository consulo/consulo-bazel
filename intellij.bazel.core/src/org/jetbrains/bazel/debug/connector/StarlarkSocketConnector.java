package org.jetbrains.bazel.debug.connector;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest;
import consulo.application.progress.ProgressManager;
import org.jetbrains.annotations.TestOnly;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class StarlarkSocketConnector implements Closeable {
    private final Socket mySocket;
    private final InputStream myIns;
    private final OutputStream myOuts;

    private StarlarkSocketConnector(Socket socket) throws IOException {
        mySocket = socket;
        myIns = socket.getInputStream();
        myOuts = socket.getOutputStream();
    }

    public void write(DebugRequest request) throws IOException {
        request.writeDelimitedTo(myOuts);
    }

    public DebugEvent read() throws IOException {
        if (mySocket.isClosed()) {
            throw new IOException("Socket closed");
        }
        DebugEvent event = DebugEvent
            .parseDelimitedFrom(myIns); // will be null when EOF is reached
        if (event == null) {
            throw new IOException("Socket stream is closed");
        }
        return event;
    }

    /**
     * This function needs to be safe to call multiple times
     */
    @Override
    public void close() {
        if (!mySocket.isClosed()) {
            try {
                mySocket.close();
            }
            catch (IOException e) {
                // consulo: Kotlin has no checked exceptions, the IOException is propagated unchecked
                throw new UncheckedIOException(e);
            }
        }
    }

    // consulo: suspend fun -> CompletableFuture; the connection is established on the calling (background) thread
    public static CompletableFuture<StarlarkSocketConnector> tryConnectTo(
        int port,
        int attempts,
        long intervalMillis
    ) {
        try {
            return CompletableFuture.completedFuture(new StarlarkSocketConnector(establishSocket(port, attempts, intervalMillis)));
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * @throws ConnectException if the last attempt fails to connect
     */
    private static Socket establishSocket(
        int port,
        int attempts,
        long intervalMillis
    ) throws IOException {
        for (int i = 0; i < attempts - 1; i++) {
            try {
                return new Socket(HOST, port);
            }
            catch (IOException ignored) {
                // ignore IOExceptions on all but the last attempt; other exceptions are thrown normally
                delay(intervalMillis);
            }
        }
        // this is the last attempt, escalate any exception that occurs
        return new Socket(HOST, port);
    }

    // consulo: counterpart of the cancellable kotlinx.coroutines.delay
    private static void delay(long intervalMillis) {
        try {
            Thread.sleep(intervalMillis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }
        ProgressManager.checkCanceled();
    }

    @TestOnly
    public static StarlarkSocketConnector simpleSocketConnection(Socket socket) {
        try {
            return new StarlarkSocketConnector(socket);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final String HOST = "localhost";
}
