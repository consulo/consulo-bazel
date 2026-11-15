package org.jetbrains.bazel.golang.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: upstream GoWorkspaceImporter.Factory (registered as syncWorkspaceImporterFactory),
// top-level because an @ExtensionImpl cannot be a nested class
@ExtensionImpl
public class GoWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new GoWorkspaceImporter(context);
    }
}
