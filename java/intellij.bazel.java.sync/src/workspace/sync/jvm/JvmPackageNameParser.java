package org.jetbrains.bazel.sync.workspace.languages.jvm;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cheaply extracts the package name declared in a JVM (Java, Kotlin, Scala) source file without parsing it.
 * <p>
 * The file is scanned line by line for {@code package <name>} declarations. To keep sync fast on large projects, the file is
 * read with a small buffer and a one-byte charset; only lines that look like a package declaration are re-decoded
 * as UTF-8, so non-ASCII package names are still recognized.
 * <p>
 * This is a heuristic: comments, string literals and annotations on the package clause are not taken into account.
 */
public final class JvmPackageNameParser {
    private JvmPackageNameParser() {
    }

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\p{L}0-9_.]+)");
    private static final Charset ONE_BYTE_CHARSET = Charset.forName("ISO-8859-1");
    private static final int BUFFER_SIZE = 256; // Should be enough to read a Java package name if it's on the first line

    // consulo: overload for the upstream default value of multipleLines
    public static @Nullable String findPackage(Path source) {
        return findPackage(source, false);
    }

    /**
     * Returns the package of {@code source}, or {@code null} if the file does not exist or declares no package.
     * <p>
     * If {@code multipleLines} is {@code true}, all package declarations of the file are joined with {@code .},
     * which supports Scala chained package clauses ({@code package a} followed by {@code package b} means {@code a.b});
     * otherwise, the first declaration wins.
     */
    public static @Nullable String findPackage(Path source, boolean multipleLines) {
        // avoid extra stat per file by handling exception
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(Files.newInputStream(source), ONE_BYTE_CHARSET), BUFFER_SIZE);
        }
        catch (NoSuchFileException e) {
            return null;
        }
        catch (IOException e) {
            // consulo: Kotlin has no checked exceptions
            throw new UncheckedIOException(e);
        }
        try (BufferedReader bufferedReader = reader) {
            // Not using UTF-8 charset because it is slower to decode
            List<String> packages = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.stripLeading().startsWith("package")) {
                    continue;
                }
                String decodedLine = new String(line.getBytes(ONE_BYTE_CHARSET), StandardCharsets.UTF_8);
                Matcher matcher = PACKAGE_PATTERN.matcher(decodedLine);
                if (!matcher.find()) {
                    continue;
                }
                String value = matcher.group(1);
                if (value == null) {
                    continue;
                }
                if (!multipleLines) {
                    // consulo: Kotlin lazy sequence firstOrNull stops at the first declaration
                    return value;
                }
                packages.add(value);
            }
            if (multipleLines) {
                String joined = String.join(".", packages);
                return joined.isEmpty() ? null : joined;
            }
            else {
                return null;
            }
        }
        catch (IOException e) {
            // consulo: Kotlin has no checked exceptions
            throw new UncheckedIOException(e);
        }
    }
}
