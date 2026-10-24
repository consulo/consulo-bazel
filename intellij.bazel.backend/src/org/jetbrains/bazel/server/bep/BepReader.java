package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import org.jetbrains.bazel.server.bsp.utils.DelimitedMessageStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BepReader {
    private final BepServer myBepServer;
    private final Path myEventFile;
    private final AtomicLong myServerPid = new AtomicLong(0);

    private final AtomicBoolean myBazelBuildFinished = new AtomicBoolean(false);

    public BepReader(BepServer bepServer, Path eventFile) {
        myBepServer = bepServer;
        myEventFile = eventFile;
    }

    public BepServer getBepServer() {
        return myBepServer;
    }

    public Path getEventFile() {
        return myEventFile;
    }

    public AtomicLong getServerPid() {
        return myServerPid;
    }

    public CompletableFuture<Void> start() {
        LOG.info("Start listening to BEP events");
        setFilePermissions(myEventFile);
        BufferedInputStream inputStream;
        try {
            inputStream = new BufferedInputStream(Files.newInputStream(myEventFile));
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        // consulo: upstream `use { }` closes the stream when reading finishes, also on failure
        return readBepEvents(inputStream)
            .whenComplete((result, error) -> {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    if (error == null) {
                        throw new CompletionException(e);
                    }
                    error.addSuppressed(e);
                }
            })
            .thenRun(() -> LOG.info("BEP events listening finished"));
    }

    private CompletableFuture<Void> readBepEvents(BufferedInputStream inputStream) {
        Parser<BuildEventStreamProtos.BuildEvent> parser = BuildEventStreamProtos.BuildEvent.parser();
        return readNextBepEvent(inputStream, parser);
    }

    // consulo: one iteration of the upstream `while (true)` loop of readBepEvents(); the next iteration is scheduled
    // on the executor, so a long event stream does not grow the stack
    private CompletableFuture<Void> readNextBepEvent(
        BufferedInputStream inputStream,
        Parser<BuildEventStreamProtos.BuildEvent> parser
    ) {
        return DelimitedMessageStream.awaitDelimitedMessageBytes(inputStream, TIMEOUT, POLL_INTERVAL, () -> myBazelBuildFinished.get())
            .thenComposeAsync(eventBytes -> {
                if (eventBytes == null) {
                    return CompletableFuture.completedFuture(null);
                }
                BuildEventStreamProtos.BuildEvent event;
                try {
                    event = parser.parsePartialFrom(eventBytes);
                }
                catch (InvalidProtocolBufferException e) {
                    throw new CompletionException(e);
                }
                myBepServer.handleBuildEventStreamProtosEvent(event);
                setServerPid(event);
                return readNextBepEvent(inputStream, parser);
            }, Application.get().getInstance(ApplicationConcurrency.class).executor());
    }

    private void setServerPid(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasStarted()) {
            myServerPid.compareAndSet(0, event.getStarted().getServerPid());
        }
    }

    public void bazelBuildFinished() {
        myBazelBuildFinished.set(true);
    }

    private static final Logger LOG = Logger.getInstance(BepReader.class);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(10);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static void setFilePermissions(Path receiver) {
        File file = receiver.toFile();
        file.setReadable(true, true);
        file.setWritable(true, true);
    }
}
