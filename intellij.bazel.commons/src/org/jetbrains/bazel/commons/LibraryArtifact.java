package org.jetbrains.bazel.commons;

import org.jspecify.annotations.Nullable;

import java.util.List;

record LibraryArtifact(
    @Nullable ArtifactLocation interfaceJar,
    @Nullable ArtifactLocation classJar,
    List<ArtifactLocation> sourceJars
) {
}
