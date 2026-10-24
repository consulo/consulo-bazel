package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IntelliJRunnerOutputParser implements Parser {
    public static final IntelliJRunnerOutputParser INSTANCE = new IntelliJRunnerOutputParser();

    private IntelliJRunnerOutputParser() {
    }

    // consulo: a Java text block strips the common indentation like Kotlin trimIndent()
    private static final Pattern HEADLINE_REGEX = Pattern.compile(
        """
          ^
          \\s*
          (?<runner>[A-Za-z0-9\\s._-]+[Rr][Uu][Nn][Nn][Ee][Rr]): # headline must contain 'runner' with any casing
          \\s*
          (?<severity>\\S+):
          \\s*
          (?<message>.*)
          $
          """,
        Pattern.COMMENTS
    );
    private static final Pattern LOCATION_LINE_REGEX = Pattern.compile(
        """
          ^
          \\s*
          (?<path>\\S(?:.*\\S)?)
          :
          (?<line>\\d+):(?<column>\\d+)
          \\s*
          $
          """,
        Pattern.COMMENTS
    );

    @Override
    public List<Diagnostic> tryParse(Output output) {
        List<String> peeked = output.peekAll();
        if (peeked.isEmpty()) {
            return List.of();
        }
        String headlineText = peeked.get(0);
        Matcher headlineMatch = HEADLINE_REGEX.matcher(headlineText);
        if (!headlineMatch.matches()) {
            return List.of();
        }
        DiagnosticSeverity severity;
        switch (headlineMatch.group(2)) {
            case "Error" -> severity = DiagnosticSeverity.ERROR;
            case "Warning" -> severity = DiagnosticSeverity.WARNING;
            default -> {
                return List.of();
            }
        }
        StringBuilder message = new StringBuilder(headlineMatch.group(3));
        for (int i = 1; i <= peeked.size() - 1; i++) {
            String textLine = peeked.get(i);
            Matcher locationMatch = LOCATION_LINE_REGEX.matcher(textLine);
            if (!locationMatch.matches()) {
                message.append('\n');
                message.append(textLine);
                continue;
            }
            String path = locationMatch.group(1);
            String lineText = locationMatch.group(2);
            String columnText = locationMatch.group(3);
            output.take(i + 1);
            return List.of(
                new Diagnostic(
                    Position.fromHumanReadable(Integer.parseInt(lineText), Integer.parseInt(columnText)),
                    message.toString(),
                    Path.of(path),
                    output.getTargetLabel(),
                    severity
                )
            );
        }
        return List.of();
    }
}
