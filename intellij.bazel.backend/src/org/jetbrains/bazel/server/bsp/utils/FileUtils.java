package org.jetbrains.bazel.server.bsp.utils;

import consulo.util.io.PathKt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtils {
    private FileUtils() {
    }

    /**
     * Important for aspect files, as writing the same content
     * updates filesystem's modification date and trigger Bazel's
     * "Checking cached action" step
     */
    public static void writeIfDifferent(Path receiver, String fileContent) {
        try {
            // Make sure this path is a file before writing into it.
            if (Files.isDirectory(receiver)) {
                PathKt.delete(receiver);
            }
            if (!Files.exists(receiver) || !Files.readString(receiver, StandardCharsets.UTF_8).equals(fileContent)) {
                Files.writeString(receiver, fileContent, StandardCharsets.UTF_8);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
