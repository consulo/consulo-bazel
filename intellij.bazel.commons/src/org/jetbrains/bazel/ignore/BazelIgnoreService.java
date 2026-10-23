package org.jetbrains.bazel.ignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.nio.file.Path;

@ServiceAPI(ComponentScope.PROJECT)
public interface BazelIgnoreService {
    boolean isIgnored(Path path);

    default boolean isIgnored(VirtualFile file) {
        return isIgnored(Path.of(file.getPath()));
    }

    static BazelIgnoreService getInstance(Project project) {
        return project.getInstance(BazelIgnoreService.class);
    }
}
