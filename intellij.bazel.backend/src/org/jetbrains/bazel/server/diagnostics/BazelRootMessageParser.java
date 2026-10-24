package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.server.diagnostics.Parser.PATH_PART;

final class BazelRootMessageParser implements Parser {
    public static final BazelRootMessageParser INSTANCE = new BazelRootMessageParser();

    private static final String TARGET_LABEL = "(//[\\w/.-]*:[\\w/.-]+)";

    private BazelRootMessageParser() {
    }

    @Override
    public List<Diagnostic> tryParse(Output output) {
        List<Diagnostic> errorInBUILD = findErrorInBUILD(output);
        if (errorInBUILD != null) {
            return errorInBUILD;
        }
        List<Diagnostic> warningsInInfoMessage = findWarningsInInfoMessage(output);
        return warningsInInfoMessage != null ? warningsInInfoMessage : List.of();
    }

    // .*(pattern)?.* will never match `pattern` as it is optional
    // workaround is to wrap it into non capturing group (?:.*(pattern)).*
    // This approach was used to find optional target label inside the message
    private static final Pattern ERROR_IN_BUILD = Pattern.compile(
        """
          ^                    # start of line
          ERROR:\\              # error indicator
          (%s/BUILD)   # path to BUILD file (1)
          :(\\d+)               # line number (2)
          (?::(\\d+))?          # optional column number (3)
          :\\                   # ": " separator
          (                    # beginning of the error message (4)
          (?:.*                # part of actual error message wrapped with label into optional group
          %s        # target label (5)
          )?                   # make target label optional
          .*                   # part of actual error message
          )                    # end of the error message (4)
          $                    # end of line
          """.formatted(PATH_PART, TARGET_LABEL),
        Pattern.COMMENTS
    );

    private @Nullable List<Diagnostic> findErrorInBUILD(Output output) {
        Matcher match = output.tryTake(ERROR_IN_BUILD);
        if (match == null) {
            return null;
        }
        List<Diagnostic> diagnostics = collectCompilerDiagnostics(output);
        return !diagnostics.isEmpty() ? diagnostics : List.of(createError(match, output.getTargetLabel()));
    }

    private Diagnostic createError(Matcher match, Label targetLabel) {
        String path = match.group(1);
        int line = Integer.parseInt(match.group(2));
        Integer columnNumber = toIntOrNull(match.group(3));
        int column = columnNumber != null ? columnNumber : 1;
        String message = match.group(4);
        return new Diagnostic(Position.fromHumanReadable(line, column), message, Path.of(path), targetLabel, DiagnosticSeverity.ERROR);
    }

    private static final Pattern INFO_MESSAGE = Pattern.compile(
        """
          ^               # start of line
          INFO:\\          # info indicator
          .*?             # part of actual message
          %s   # target label (1)
          .*              # part of actual message
          $               # end of line
        """.formatted(TARGET_LABEL),
        Pattern.COMMENTS
    );

    private @Nullable List<Diagnostic> findWarningsInInfoMessage(Output output) {
        Matcher match = output.tryTake(INFO_MESSAGE);
        if (match == null) {
            return null;
        }
        return collectCompilerDiagnostics(output);
    }

    private List<Diagnostic> collectCompilerDiagnostics(Output output) {
        List<Diagnostic> result = new ArrayList<>();
        while (true) {
            Diagnostic diagnostic = CompilerDiagnosticParser.INSTANCE.tryParseOne(output);
            if (diagnostic == null) {
                diagnostic = Scala3CompilerDiagnosticParser.INSTANCE.tryParseOne(output);
            }
            if (diagnostic == null) {
                break;
            }
            result.add(diagnostic);
        }
        return result;
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
