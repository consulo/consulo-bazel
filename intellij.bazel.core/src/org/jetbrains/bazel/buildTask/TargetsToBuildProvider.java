package org.jetbrains.bazel.buildTask;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;

import java.util.List;

// consulo: upstream declares it in BazelProjectTaskRunner.kt; own file to keep the FQN. ModuleEntity is the classic Module
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TargetsToBuildProvider {
    List<Label> getTargetsToBuild(Project project, Module moduleEntity);

    static List<TargetsToBuildProvider> getEp() {
        return Application.get().getExtensionList(TargetsToBuildProvider.class);
    }
}
