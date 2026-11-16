package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

interface DiagnosticsParser {
    List<Diagnostic> parse(
        List<String> bazelOutputLines,
        Label target,
        boolean isCommandLineFormattedOutput,
        boolean onlyFromParsedOutput
    );

    // consulo: upstream default values of isCommandLineFormattedOutput and onlyFromParsedOutput
    default List<Diagnostic> parse(List<String> bazelOutputLines, Label target) {
        return parse(bazelOutputLines, target, false, false);
    }
}

final class DiagnosticsParserImpl implements DiagnosticsParser {
    @Override
    public List<Diagnostic> parse(
        List<String> bazelOutputLines,
        Label target,
        boolean isCommandLineFormattedOutput,
        boolean onlyFromParsedOutput
    ) {
        Output output = prepareOutput(bazelOutputLines, target);
        List<Diagnostic> diagnostics = collectDiagnostics(output, isCommandLineFormattedOutput, onlyFromParsedOutput);
        return deduplicate(diagnostics);
    }

    private Output prepareOutput(List<String> lines, Label target) {
        List<String> relevantLines = lines.stream()
            .filter(it -> !it.isEmpty())
            .toList();
        return new Output(relevantLines, target);
    }

    private List<Diagnostic> collectDiagnostics(Output output, boolean isCommandLineFormattedOutput, boolean onlyFromParsedOutput) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        while (output.nonEmpty()) {
            List<Parser> parsers = isCommandLineFormattedOutput ? COMMAND_LINE_OUTPUT_PARSER : PARSERS;
            for (Parser parser : parsers) {
                List<Diagnostic> result = parser.tryParse(output);
                if (!result.isEmpty()) {
                    diagnostics.addAll(result);
                    break;
                }
            }
        }

        if (diagnostics.isEmpty() && !onlyFromParsedOutput) {
            diagnostics.add(
                new Diagnostic(
                    Position.NONE,
                    output.fullOutput(),
                    null,
                    output.getTargetLabel()
                )
            );
        }

        return List.copyOf(diagnostics);
    }

    private List<Diagnostic> deduplicate(List<Diagnostic> parsedDiagnostics) {
        // consulo: a list is the group key in place of Kotlin Triple(fileLocation, message, position)
        Map<List<@Nullable Object>, List<Diagnostic>> groups = new LinkedHashMap<>();
        for (Diagnostic it : parsedDiagnostics) {
            List<@Nullable Object> key = Arrays.asList(it.fileLocation(), it.message(), it.position());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
        }
        List<Diagnostic> result = new ArrayList<>();
        for (List<Diagnostic> it : groups.values()) {
            result.add(it.get(0));
        }
        return result;
    }

    private static final List<Parser> PARSERS =
        List.of(
            CompilerDiagnosticParser.INSTANCE,
            IntelliJRunnerOutputParser.INSTANCE,
            Scala3CompilerDiagnosticParser.INSTANCE,
            AllCatchParser.INSTANCE
        );
    private static final List<Parser> COMMAND_LINE_OUTPUT_PARSER =
        List.of(
            BazelOutputMessageParser.INSTANCE
        );
}
