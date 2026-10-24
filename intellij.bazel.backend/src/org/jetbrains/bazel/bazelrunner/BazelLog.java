package org.jetbrains.bazel.bazelrunner;

import consulo.container.boot.ContainerPathManager;
import org.jetbrains.bazel.config.BazelFeatureFlags;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.function.Consumer;

public final class BazelLog {
    private BazelLog() {
    }

    public static Path getLogPath() {
        return ContainerPathManager.get().getLogPath().toPath().resolve("bazel-logs").resolve("bazel.log");
    }

    public static void write(Consumer<PrintWriter> body) {
        if (!BazelFeatureFlags.isLogEnabled()) {
            return;
        }

        Path logPath = getLogPath();
        // consulo: IntelliJ Path.outputStream(append = true) also creates the parent directories
        try {
            Files.createDirectories(logPath.getParent());
            try (OutputStream out = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                try (PrintWriter writer = new PrintWriter(out)) {
                    try {
                        writer.append("\n").append(Instant.now().toString()).append('\n');
                        body.accept(writer);
                    }
                    catch (Throwable ex) {
                        ex.printStackTrace(writer);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
