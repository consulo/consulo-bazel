package org.jetbrains.bazel.workspace.importer;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: upstream JavaBazelWorkspaceImporter.Factory (registered as syncWorkspaceImporterFactory),
// top-level because an @ExtensionImpl cannot be a nested class
@ExtensionImpl
public class JavaBazelWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new JavaBazelWorkspaceImporter(context);
    }
}
