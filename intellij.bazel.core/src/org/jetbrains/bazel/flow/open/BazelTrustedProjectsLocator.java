package org.jetbrains.bazel.flow.open;

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// TODO consulo: TrustedProjectsLocator (trusted projects API) is missing in Consulo; the class is not registered
public class BazelTrustedProjectsLocator {
    public List<Path> getProjectRoots(Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try {
            result.add(org.jetbrains.bazel.config.project.getRootDir(project).toNioPath());
        }
        catch (UnsupportedOperationException ignored) {
            // consulo: upstream toNioPathOrNull()
        }
        return result;
    }

    public List<Path> getProjectRoots(Path projectRoot, @Nullable Project project) {
        // 'projectRoot' despite its name, may be a file when bazel plugin opens MODULE.bazel, *.bazelproject, etc.
        utils.ResolvedProjectRoot resolvedProjectRoot = utils.ResolvedProjectRoot.from(projectRoot);
        return resolvedProjectRoot == null ? List.of() : List.of(resolvedProjectRoot.getRootDir());
    }
}
