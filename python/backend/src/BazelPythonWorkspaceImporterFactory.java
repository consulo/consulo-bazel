package com.intellij.bazel.python.backend;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: upstream BazelPythonWorkspaceImporter.Factory (registered as syncWorkspaceImporterFactory),
// top-level because an @ExtensionImpl cannot be a nested class
@ExtensionImpl
public class BazelPythonWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new BazelPythonWorkspaceImporter(context);
    }
}
