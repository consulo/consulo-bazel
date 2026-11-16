package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

// TODO consulo: CIDR extension point cidr.lang.projectModel.resolveConfigurationDisplayNameProvider
//  (OCResolveConfigurationDisplayNameProvider) does not exist in consulo-cpp, the class is not registered
public class CcResolveConfigurationDisplayNameProvider {
    public CcResolveConfigurationDisplayNameProvider() {
    }

    // consulo: CIDR OCResolveConfiguration is CcWorkspaceModel.Configuration, which has no project: it is a parameter
    public @Nullable String getDisplayName(Project project, CcWorkspaceModel.Configuration configuration, VirtualFile sourceFile) {
        CcResolveConfiguration.Identifier identifier = buildEquivalenceClasses.getCcIdentifier(configuration);
        if (identifier == null) {
            return null;
        }
        // consulo: VirtualFile.toNioPathOrNull(); Consulo's toNioPath() throws for a file without a path
        Path path;
        try {
            path = sourceFile.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
        WorkspaceSnapshot snapshot = project.getInstance(WorkspaceSnapshotService.class).getSnapshot().get();

        List<String> displayNames = snapshot.fileToTarget().getTargetsByFile(path).stream()
            .filter(it -> it.configuration().equals(identifier.getConfigurationId()))
            .map(it -> buildEquivalenceClasses.displayName(it, snapshot.configurations()))
            .distinct()
            .toList();
        // consulo: Kotlin singleOrNull()
        return displayNames.size() == 1 ? displayNames.get(0) : null;
    }
}
