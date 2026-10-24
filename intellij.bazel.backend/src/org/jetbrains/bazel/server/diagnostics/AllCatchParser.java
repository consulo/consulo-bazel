package org.jetbrains.bazel.server.diagnostics;

import java.util.List;

final class AllCatchParser implements Parser {
    public static final AllCatchParser INSTANCE = new AllCatchParser();

    private AllCatchParser() {
    }

    @Override
    public List<Diagnostic> tryParse(Output output) {
        output.tryTake();
        return List.of();
    }
}
