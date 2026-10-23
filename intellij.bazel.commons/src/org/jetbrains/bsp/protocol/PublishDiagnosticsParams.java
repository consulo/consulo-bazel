package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.diagnostics.Diagnostic;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record PublishDiagnosticsParams(
    TaskId taskId,
    @Nullable TextDocumentIdentifier textDocument,
    Label buildTarget,
    List<Diagnostic> diagnostics,
    boolean reset
) {
}
