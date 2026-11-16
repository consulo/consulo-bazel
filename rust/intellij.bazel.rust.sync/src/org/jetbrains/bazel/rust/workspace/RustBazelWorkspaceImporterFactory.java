package org.jetbrains.bazel.rust.workspace;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: Consulo-only, like GoWorkspaceImporterFactory (a top-level class: an @ExtensionImpl cannot be nested)
@ExtensionImpl
public class RustBazelWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new RustBazelWorkspaceImporter(context);
    }
}
