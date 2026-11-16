package org.jetbrains.bazel.rust.sync;

import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AmbiguousEmptyTarget;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * consulo: stand-in for the BSP server model {@code org.jetbrains.bazel.server.model.Module} (removed upstream), with the
 * members the Rust resolvers read. The importer creates it from a {@code BuildTarget} of the workspace snapshot.
 *
 * @param baseDirectory the directory of the Bazel package of the target, as a VFS url
 * @param languageData  the {@link RustModule} of the target
 */
public record Module(
    Label label,
    List<Label> directDependencies,
    Set<LanguageClass> languages,
    String baseDirectory,
    @Nullable RustModule languageData
) {
    /**
     * consulo: upstream {@code label.packagePath.toString()}. The Rust resolvers use it as the id of a Rust package and parse
     * {@code "$id:$targetName"} back to a label, so it has to keep the repository of the label ({@code @//example},
     * {@code @@repo//}, as the BSP Rust e2e test expected); the package path alone would merge all external crates.
     */
    public static String packagePath(Label label) {
        if (label instanceof ResolvedLabel resolvedLabel) {
            return new ResolvedLabel(resolvedLabel.repo(), resolvedLabel.packagePath(), AmbiguousEmptyTarget.INSTANCE).toString();
        }
        return label.packagePath().toString();
    }
}
