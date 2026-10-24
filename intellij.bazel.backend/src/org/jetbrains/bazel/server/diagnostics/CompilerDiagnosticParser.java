package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.server.diagnostics.Parser.PATH_PART;

final class CompilerDiagnosticParser implements Parser {
    public static final CompilerDiagnosticParser INSTANCE = new CompilerDiagnosticParser();

    private CompilerDiagnosticParser() {
    }

    @Override
    public List<Diagnostic> tryParse(Output output) {
        Diagnostic diagnostic = tryParseOne(output);
        return diagnostic != null ? List.of(diagnostic) : List.of();
    }

    // Example:
    // server/DiagnosticsServiceTest.kt:12:18: error: type mismatch: inferred type is String but Int was expected
    private static final Pattern DIAGNOSTIC_HEADER = Pattern.compile(
        """
          ^                # start of line
          (%s)     # file path (1)
          :(\\d+)           # line number (2)
          (?::(\\d+))?      # optional column number (3)
          :\\               # ": " separator
          ([a-zA-Z\\ ]+):\\  # level (4)
          (.*)             # actual error message (5)
          $                # end of line
          """.formatted(PATH_PART),
        Pattern.COMMENTS
    );

    public @Nullable Diagnostic tryParseOne(Output output) {
        Matcher match = output.tryTake(DIAGNOSTIC_HEADER);
        if (match == null) {
            return null;
        }
        String path = match.group(1);
        int line = Integer.parseInt(match.group(2));
        List<String> messageLines = collectMessageLines(match.group(5), output);
        Integer columnNumber = toIntOrNull(match.group(3));
        if (columnNumber == null) {
            columnNumber = tryFindColumnNumber(messageLines);
        }
        int column = columnNumber != null ? columnNumber : 1;
        DiagnosticSeverity level = "warning".equals(match.group(4)) ? DiagnosticSeverity.WARNING : DiagnosticSeverity.ERROR;
        String message = String.join("\n", messageLines);
        return new Diagnostic(Position.fromHumanReadable(line, column), message, Path.of(path), output.getTargetLabel(), level);
    }

    private List<String> collectMessageLines(String header, Output output) {
        List<String> lines = new ArrayList<>();
        lines.addAll(tryCollectLinesMatchingIssueDetails(output));
        if (lines.isEmpty()) {
            lines.addAll(tryCollectLinesTillErrorMarker(output));
        }
        lines.add(0, header);
        return lines;
    }

    private static final Pattern ISSUE_POSITION_MARKER = Pattern.compile("^\\s*\\^+\\s*$"); // any number of ^ surrounded by whitespace only

    private List<String> tryCollectLinesTillErrorMarker(Output output) {
        List<String> peeked = output.peek(30);
        int issuePositionMarkerIndex = indexOfFirst(peeked, ISSUE_POSITION_MARKER);
        if (issuePositionMarkerIndex != -1) {
            int nextDiagnosticIndex = indexOfFirst(peeked, DIAGNOSTIC_HEADER);
            if (nextDiagnosticIndex == -1) {
                nextDiagnosticIndex = Integer.MAX_VALUE;
            }
            return output.take(Math.min(issuePositionMarkerIndex + 1, nextDiagnosticIndex));
        }
        else {
            return List.of();
        }
    }

    private static final Pattern ISSUE_DETAILS = Pattern.compile("^\\s+.*|" + ISSUE_POSITION_MARKER.pattern()); // indented line or ^

    private List<String> tryCollectLinesMatchingIssueDetails(Output output) {
        List<String> result = new ArrayList<>();
        while (true) {
            Matcher match = output.tryTake(ISSUE_DETAILS);
            if (match == null) {
                break;
            }
            result.add(match.group());
        }
        return result;
    }

    private @Nullable Integer tryFindColumnNumber(List<String> messageLines) {
        for (String line : messageLines) {
            if (ISSUE_POSITION_MARKER.matcher(line).matches()) {
                return line.indexOf("^") + 1;
            }
        }
        return null;
    }

    // consulo: Kotlin List.indexOfFirst { regex.matches(it) }
    private static int indexOfFirst(List<String> lines, Pattern regex) {
        for (int i = 0; i < lines.size(); i++) {
            if (regex.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        return -1;
    }

    // consulo: Kotlin MatchResult.groupValues[i].toIntOrNull(), an unmatched group is an empty string there
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
