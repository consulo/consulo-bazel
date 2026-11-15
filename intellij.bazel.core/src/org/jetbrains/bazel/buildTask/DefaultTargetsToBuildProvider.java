package org.jetbrains.bazel.buildTask;

import consulo.annotation.component.ExtensionImpl;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.workspacemodel.entities.extensions;

import java.util.List;

// consulo: upstream declares it in BazelProjectTaskRunner.kt; @ExtensionImpl classes need their own file
@ExtensionImpl
public class DefaultTargetsToBuildProvider implements TargetsToBuildProvider {
    @Override
    public List<Label> getTargetsToBuild(Project project, Module moduleEntity) {
        extensions.BazelModuleExtensionEntity extension = extensions.getBazelModuleExtension(moduleEntity);
        return extension == null ? List.of() : List.of(extension.getTargetKey().label());
    }
}
