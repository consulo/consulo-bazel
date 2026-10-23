package org.jetbrains.bazel.languages.starlark.psi.statements;

import java.util.List;

public interface StarlarkStatementContainer {
    List<StarlarkStatementList> getStatementLists();
}
