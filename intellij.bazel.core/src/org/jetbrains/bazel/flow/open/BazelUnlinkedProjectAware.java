package org.jetbrains.bazel.flow.open;

import consulo.application.ReadAction;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

// TODO consulo: ExternalSystemUnlinkedProjectAware (auto-link of unlinked projects) is missing in Consulo; the class is not
// registered. ExternalSystemProjectLinkListener and the subscribe(...) override are dropped with it.
public class BazelUnlinkedProjectAware {
    private static final Logger LOG = Logger.getInstance(BazelUnlinkedProjectAware.class);

    public ProjectSystemId getSystemId() {
        return BazelPluginConstants.SYSTEM_ID;
    }

    public boolean isBuildFile(Project project, VirtualFile buildFile) {
        return utils.isBazelWorkspaceFile(buildFile);
    }

    public boolean isLinkedProject(Project project, String externalProjectPath) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    public CompletableFuture<?> linkAndLoadProjectAsync(Project project, String externalProjectPath) {
        return BazelApplicationCoroutineScopeService.getInstance().launch(() -> {
            @Nullable Path file = ReadAction.<@Nullable Path, RuntimeException>compute(() -> {
                VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(externalProjectPath);
                if (projectDir == null) {
                    return null;
                }
                VirtualFile[] children = projectDir.getChildren();
                if (children == null) {
                    return null;
                }
                for (VirtualFile child : children) {
                    if (isBuildFile(project, child)) {
                        try {
                            return child.toNioPath();
                        }
                        catch (UnsupportedOperationException e) {
                            return null;
                        }
                    }
                }
                return null;
            });

            if (file != null) {
                return utils.closeAndReopenAsBazelProject(project, file);
            }
            else {
                LOG.warn("Unable to find Bazel project file for " + externalProjectPath);
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    public CompletableFuture<?> unlinkProject(Project project, String externalProjectPath) {
        // This method must be implemented as it is a part of an API.
        // If was mandatory from the very beginning, this assertion sat in a parent.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
