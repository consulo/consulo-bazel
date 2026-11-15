package org.jetbrains.bazel.clion.workspace;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: upstream CcWorkspaceImporter.Factory (registered as syncWorkspaceImporterFactory),
// top-level because an @ExtensionImpl cannot be a nested class
@ExtensionImpl
public class CcWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new CcWorkspaceImporter(context);
    }
}
