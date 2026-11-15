package org.jetbrains.bazel.golang.coverage;

import consulo.application.progress.ProgressManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GoCoverageReportParser {
    private GoCoverageReportParser() {
    }

    // consulo: the Kotlin function type (String, Int, Long) -> Unit of the consumer
    @FunctionalInterface
    public interface CoverageLineConsumer {
        void accept(String filePath, int lineNumber, long hits);
    }

    private static final Pattern blockRegex = Pattern.compile("^(.+):(\\d+)\\.\\d+,(\\d+)\\.\\d+\\s+\\d+\\s+(\\d+)$");
    private static final String MODE_PREFIX = "mode: ";
    private static final long MAX_COVERAGE_BLOCK_LINES = 1_000_000L;

    // consulo: overload for the upstream default argument onMalformedLine = {}
    public static boolean parse(Path file, CoverageLineConsumer consumer) {
        return parse(file, line -> {
        }, consumer);
    }

    public static boolean parse(
        Path file,
        Consumer<String> onMalformedLine,
        CoverageLineConsumer consumer
    ) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try (Stream<String> lines = Files.lines(file)) {
            return parse(lines, onMalformedLine, consumer);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // consulo: overload for the upstream default argument onMalformedLine = {}
    public static boolean parse(Stream<String> lines, CoverageLineConsumer consumer) {
        return parse(lines, line -> {
        }, consumer);
    }

    // consulo: Kotlin Sequence<String> -> Stream<String>
    public static boolean parse(
        Stream<String> lines,
        Consumer<String> onMalformedLine,
        CoverageLineConsumer consumer
    ) {
        Iterator<String> iterator = lines.iterator();
        if (!iterator.hasNext() || !isModeLine(iterator.next())) {
            return false;
        }

        while (iterator.hasNext()) {
            ProgressManager.checkCanceled();
            String line = iterator.next();
            Matcher match = blockRegex.matcher(line);
            if (!match.matches()) {
                onMalformedLine.accept(line);
                continue;
            }
            String filePath = match.group(1);
            Integer startLine = toIntOrNull(match.group(2));
            Integer endLine = toIntOrNull(match.group(3));
            Long hits = toLongOrNull(match.group(4));
            if (startLine == null || endLine == null || hits == null) {
                onMalformedLine.accept(line);
                continue;
            }
            long blockLines = endLine.longValue() - startLine + 1;
            if (blockLines < 1L || blockLines > MAX_COVERAGE_BLOCK_LINES) {
                onMalformedLine.accept(line);
                continue;
            }

            for (int lineNumber = startLine; lineNumber <= endLine; lineNumber++) {
                ProgressManager.checkCanceled();
                consumer.accept(filePath, lineNumber, hits);
            }
        }
        return true;
    }

    private static boolean isModeLine(String receiver) {
        return receiver.startsWith(MODE_PREFIX);
    }

    // consulo: Kotlin String.toIntOrNull() / toLongOrNull()
    private static @Nullable Integer toIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Long toLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
