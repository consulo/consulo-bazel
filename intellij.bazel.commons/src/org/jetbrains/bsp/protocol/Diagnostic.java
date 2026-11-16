package org.jetbrains.bazel.server.diagnostics;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.Position;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public record Diagnostic(
    Position position,
    String message,
    @Nullable Path fileLocation,
    Label targetLabel,
    @Nullable DiagnosticSeverity level
) {
    public Diagnostic(Position position, String message, @Nullable Path fileLocation, Label targetLabel) {
        this(position, message, fileLocation, targetLabel, null);
    }

    public Diagnostic demoteErrors() {
        return new Diagnostic(position, message, fileLocation, targetLabel, level == DiagnosticSeverity.ERROR ? DiagnosticSeverity.WARNING : level);
    }
}
