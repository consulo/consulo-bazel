package org.jetbrains.bazel.languages.starlark.index;

import consulo.index.io.ID;

import java.util.Collection;

public final class StarlarkLoadEdgesIndex {
    private StarlarkLoadEdgesIndex() {
    }

    public static final ID<String, Collection<String>> NAME = ID.create("starlark.load.edges");
}
