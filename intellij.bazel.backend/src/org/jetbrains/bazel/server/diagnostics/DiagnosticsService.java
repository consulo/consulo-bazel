package org.jetbrains.bazel.server.diagnostics;

import consulo.util.lang.Pair;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TextDocumentIdentifier;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiagnosticsService {
    private final Path myWorkspaceRoot;
    private final DiagnosticsParserImpl myParser = new DiagnosticsParserImpl();

    public DiagnosticsService(Path workspaceRoot) {
        myWorkspaceRoot = workspaceRoot;
    }

    public List<PublishDiagnosticsParams> extractDiagnostics(
        List<String> bazelOutputLines,
        Label targetLabel,
        TaskId taskId,
        boolean isCommandLineFormattedOutput,
        boolean onlyFromParsedOutput
    ) {
        List<Diagnostic> parsedDiagnostics =
            myParser.parse(bazelOutputLines, targetLabel, isCommandLineFormattedOutput, onlyFromParsedOutput);
        return mapDiagnostics(parsedDiagnostics, taskId);
    }

    // consulo: upstream default values of isCommandLineFormattedOutput and onlyFromParsedOutput
    public List<PublishDiagnosticsParams> extractDiagnostics(
        List<String> bazelOutputLines,
        Label targetLabel,
        TaskId taskId
    ) {
        return extractDiagnostics(bazelOutputLines, targetLabel, taskId, false, false);
    }

    private List<PublishDiagnosticsParams> mapDiagnostics(List<Diagnostic> diagnostics, TaskId taskId) {
        Map<Pair<@Nullable Path, Label>, List<Diagnostic>> groups = new LinkedHashMap<>();
        for (Diagnostic diagnostic : diagnostics) {
            Path fileLocation = diagnostic.fileLocation();
            Path path = fileLocation != null ? myWorkspaceRoot.resolve(fileLocation) : null;
            groups.computeIfAbsent(Pair.create(path, diagnostic.targetLabel()), key -> new ArrayList<>()).add(diagnostic);
        }
        List<PublishDiagnosticsParams> result = new ArrayList<>();
        for (Map.Entry<Pair<@Nullable Path, Label>, List<Diagnostic>> kv : groups.entrySet()) {
            Path path = kv.getKey().getFirst();
            List<Diagnostic> mappedDiagnostics = new ArrayList<>();
            for (Diagnostic it : kv.getValue()) {
                mappedDiagnostics.add(new Diagnostic(it.position(), it.message(), path, it.targetLabel(), it.level()));
            }
            TextDocumentIdentifier doc = path != null ? new TextDocumentIdentifier(path) : null;
            result.add(new PublishDiagnosticsParams(taskId, doc, kv.getKey().getSecond(), mappedDiagnostics, true));
        }
        return result;
    }
}
