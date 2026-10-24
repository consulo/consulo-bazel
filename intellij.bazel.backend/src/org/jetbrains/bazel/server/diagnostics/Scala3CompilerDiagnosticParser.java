package org.jetbrains.bazel.server.diagnostics;

import consulo.util.lang.StringUtil;
import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.server.diagnostics.Parser.PATH_PART;

final class Scala3CompilerDiagnosticParser implements Parser {
    public static final Scala3CompilerDiagnosticParser INSTANCE = new Scala3CompilerDiagnosticParser();

    private Scala3CompilerDiagnosticParser() {
    }

    @Override
    public List<Diagnostic> tryParse(Output output) {
        Diagnostic diagnostic = tryParseOne(output);
        return diagnostic != null ? List.of(diagnostic) : List.of();
    }

    private static final Pattern DIAGNOSTIC_HEADER = Pattern.compile(
        """
          ^--\\                     # "-- " diagnostic start
          (?:\\[E\\d+\\])?            # "[E008]" optional explanation id
          ([^:]+):                 # (1) type of diagnostic
          (%s):(\\d+):(\\d+) # (2) path, (3) line, (4) column
          [\\s-]*$                  # " -----------------" ending
          """.formatted(PATH_PART),
        Pattern.COMMENTS
    );

    // Scala 3 diagnostics have additional color printed, since Bazel uses renderedMessage field
    private static final Pattern COLOR_REGEX = Pattern.compile("\u001b\\[1A\u001b\\[K|\u001B\\[[;\\d]*m");

    public @Nullable Matcher tryTake(Output output, Pattern regex) {
        String line = output.peek();
        if (line == null) {
            return null;
        }
        Matcher matcher = regex.matcher(COLOR_REGEX.matcher(line).replaceAll(""));
        if (!matcher.matches()) {
            return null;
        }
        output.take();
        return matcher;
    }

    public @Nullable Diagnostic tryParseOne(Output output) {
        Matcher match = tryTake(output, DIAGNOSTIC_HEADER);
        if (match == null) {
            return null;
        }
        DiagnosticSeverity level = match.group(1).contains("Error") ? DiagnosticSeverity.ERROR : DiagnosticSeverity.WARNING;
        String path = match.group(2).trim();
        int line = Integer.parseInt(match.group(3));
        List<String> messageLines = collectMessageLines(match.group(1).trim(), output);
        Integer columnNumber = toIntOrNull(match.group(4));
        if (columnNumber == null) {
            columnNumber = tryFindColumnNumber(messageLines);
        }
        int column = columnNumber != null ? columnNumber : 1;
        String message = String.join("\n", messageLines);
        return new Diagnostic(Position.fromHumanReadable(line, column), message, Path.of(path), output.getTargetLabel(), level);
    }

    private List<String> collectMessageLines(String header, Output output) {
        List<String> lines = new ArrayList<>();

        // skip lines with numbers which show the source and skip the next ^^^^ line
        String first = output.peek();
        if (first != null && !cleanLine(first).startsWith("|")) {
            output.take(2);
        }
        while (output.nonEmpty() && startsWithPipe(output.peek())) {
            lines.add(
                StringUtil.trimStart(
                    cleanLine(output.take()),
                    "|"
                ).trim()
            );
        }
        lines.add(0, header);
        return lines;
    }

    // consulo: Kotlin local extension function String.cleanLine()
    private static String cleanLine(String receiver) {
        return COLOR_REGEX.matcher(receiver).replaceAll("").trim();
    }

    // consulo: Kotlin output.peek()?.cleanLine()?.startsWith('|') == true
    private static boolean startsWithPipe(@Nullable String line) {
        return line != null && cleanLine(line).startsWith("|");
    }

    private static final Pattern ISSUE_POSITION_MARKER = Pattern.compile("^[\\s\\|]*\\^\\s*$"); // ^ surrounded by whitespace only

    private @Nullable Integer tryFindColumnNumber(List<String> messageLines) {
        for (String line : messageLines) {
            if (ISSUE_POSITION_MARKER.matcher(COLOR_REGEX.matcher(line).replaceAll("")).matches()) {
                return line.indexOf("^") + 1;
            }
        }
        return null;
    }

    // consulo: Kotlin String.toIntOrNull()
    private static @Nullable Integer toIntOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
