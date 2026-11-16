package org.jetbrains.bazel.target;

import consulo.project.Project;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

// consulo: upstream Kotlin top-level extension declarations; the class keeps the upstream file name
public final class BuildTargetUtils {
    private BuildTargetUtils() {
    }

    public static OutputLocation getBaseDirectoryLocation(BuildTargetInfo receiver) {
        Label label = receiver.id();
        String packagePath = label.packagePath().toString();
        if (label instanceof ResolvedLabel resolvedLabel && !label.isMainWorkspace()) {
            return new OutputLocation.External(resolvedLabel.getRepoName(), packagePath);
        }
        else {
            return new OutputLocation.Workspace(packagePath);
        }
    }

    public static @Nullable Path baseDirectory(BuildTargetInfo receiver, Project project) {
        return TargetStorage.getTargetStorage(project).resolveExecrootOutputLocation(getBaseDirectoryLocation(receiver));
    }

    public static boolean isWorkspace(BuildTargetInfo receiver, RepoMapping repoMapping) {
        Label id = receiver.id();
        if (!(id instanceof ResolvedLabel) || id.isMainWorkspace()) {
            return true;
        }
        else {
            return RepoMapping.getLocalRepositories(repoMapping).localRepositories().containsKey(((ResolvedLabel) id).getRepoName());
        }
    }

    public static OutputLocation getBaseDirectoryLocation(BuildTarget receiver) {
        return getBaseDirectoryLocation(receiver.getInfo());
    }

    public static @Nullable Path baseDirectory(BuildTarget receiver, Project project) {
        return baseDirectory(receiver.getInfo(), project);
    }

    public static boolean isWorkspace(BuildTarget receiver, RepoMapping repoMapping) {
        return isWorkspace(receiver.getInfo(), repoMapping);
    }
}
