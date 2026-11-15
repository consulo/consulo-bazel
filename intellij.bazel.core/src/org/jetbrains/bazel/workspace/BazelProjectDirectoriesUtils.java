package org.jetbrains.bazel.workspace;

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name.
// The IntelliJ workspace model is not used: BazelProjectDirectoriesEntity is project data of BazelProjectModelService and the
// upstream CachedValue over the entity storage is computed on each call
public final class BazelProjectDirectoriesUtils {
    private BazelProjectDirectoriesUtils() {
    }

    /**
     * Returns the set of excluded directories, i.e., explicit excludes from project view and Bazel symlinks.
     * Returns `null` if the project has not been synced yet or no project model is present after clearing cache/IDEA update
     */
    public static @Nullable Set<VirtualFile> excludedRoots(Project receiver) {
        BazelProjectDirectoriesEntity bazelProjectDirectories = bazelProjectDirectoriesEntity(receiver);
        if (bazelProjectDirectories == null) {
            return null;
        }
        return toVirtualFileSet(bazelProjectDirectories.excludedRoots());
    }

    /**
     * @see #excludedRoots
     */
    public static @Nullable Set<VirtualFile> includedRoots(Project receiver) {
        BazelProjectDirectoriesEntity bazelProjectDirectories = bazelProjectDirectoriesEntity(receiver);
        if (bazelProjectDirectories == null) {
            return null;
        }
        return toVirtualFileSet(bazelProjectDirectories.includedRoots());
    }

    public static @Nullable BazelProjectDirectoriesEntity bazelProjectDirectoriesEntity(Project receiver) {
        return BazelProjectModelService.getInstance(receiver).getProjectData(BazelProjectDirectoriesEntity.class);
    }

    public static Set<VirtualFile> toVirtualFileSet(List<String> receiver) {
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (String url : receiver) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
            if (file != null) {
                result.add(file);
            }
        }
        return result;
    }
}
