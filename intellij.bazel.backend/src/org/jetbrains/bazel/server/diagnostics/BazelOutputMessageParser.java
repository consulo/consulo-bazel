package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.server.diagnostics.Parser.PATH_PART;

final class BazelOutputMessageParser implements Parser {
    public static final BazelOutputMessageParser INSTANCE = new BazelOutputMessageParser();

    private static final Pattern COLOR = Pattern.compile("\\u001B\\[[0-9]+(;[0-9]+)*m");

    private BazelOutputMessageParser() {
    }

    @Override
    public List<Diagnostic> tryParse(Output output) {
        return findErrorInOutput(output);
    }

    private static final Pattern ERROR_IN_OUTPUT = Pattern.compile(
        """
          ^               # start of line
          ERROR:\\s*       # error indicator
          (.*)            # error message
          $               # end of line
          """,
        Pattern.COMMENTS
    );

    private static final Pattern FILE_IN_OUTPUT = Pattern.compile(
        """
          ^\\s*                        # start of line
          File\\s*"(?<file>[^"]*)",    # file path
          \\s*line\\s*(?<line>\\d+),     # line number
          \\s*column\\s*(?<char>\\d+)    # caret position
          .*$                         # the rest
        """,
        Pattern.COMMENTS
    );

    private static final Pattern FILE_BASED_ERROR = Pattern.compile(
        """
          ^                           # start of line
          ?(?<file>%s)        # file path
          :(?<line>\\d+)               # line number
          :(?<char>\\d+)               # caret position
          :\\s*(?<message>.+?)\\s*$     # error message
          """.formatted(PATH_PART),
        Pattern.COMMENTS
    );

    private List<Diagnostic> findErrorInOutput(Output output) {
        List<Diagnostic> result = new ArrayList<>();
        while (true) {
            String line = output.tryTake();
            if (line == null) {
                break;
            }
            String strippedLine = COLOR.matcher(line).replaceAll("");
            Matcher match = ERROR_IN_OUTPUT.matcher(strippedLine);
            if (!match.matches()) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            lines.add(match.group(1));
            while (true) {
                String peeked = output.peek();
                if (peeked == null) {
                    break;
                }
                String nextLine = COLOR.matcher(peeked).replaceAll("");
                if (ERROR_IN_OUTPUT.matcher(nextLine).matches()) {
                    break;
                }
                output.take();
                if (!nextLine.isBlank()) {
                    lines.add(nextLine);
                }
            }
            result.add(createError(lines, output.getTargetLabel()));
        }
        return result;
    }

    private Diagnostic createError(List<String> lines, Label targetLabel) {
        @Nullable Matcher fileMatch = null;
        List<String> message = new ArrayList<>();
        for (String line : lines) {
            Matcher match = FILE_BASED_ERROR.matcher(line);
            if (match.matches()) {
                fileMatch = match;
                message.add(match.group("message"));
            }
            else {
                message.add(line);
                Matcher fileInOutputMatch = FILE_IN_OUTPUT.matcher(line);
                if (fileInOutputMatch.matches()) {
                    fileMatch = fileInOutputMatch;
                }
            }
        }
        if (fileMatch != null) {
            @Nullable Path file;
            try {
                file = Paths.get(fileMatch.group("file"));
            }
            catch (InvalidPathException e) {
                file = null;
            }
            if (file != null) {
                Position position = Position.fromHumanReadable(
                    Integer.parseInt(fileMatch.group("line")),
                    Integer.parseInt(fileMatch.group("char"))
                );
                String joinedMessage = String.join("\n", message).trim();
                return new Diagnostic(position, joinedMessage, file, targetLabel, DiagnosticSeverity.ERROR);
            }
        }
        return new Diagnostic(
            Position.NONE,
            String.join("\n", message).trim(),
            null,
            targetLabel,
            DiagnosticSeverity.ERROR
        );
    }
}
