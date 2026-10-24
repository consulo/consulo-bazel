package org.jetbrains.bazel.server.diagnostics;

import java.util.List;

interface Parser {
    List<Diagnostic> tryParse(Output output);

    // regex part to match the path
    String PATH_PART = "(?:[a-zA-z]:[\\/\\\\])?[^:\\r\\n]+";
}
