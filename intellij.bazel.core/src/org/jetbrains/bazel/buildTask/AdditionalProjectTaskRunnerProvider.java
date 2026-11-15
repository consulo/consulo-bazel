package org.jetbrains.bazel.buildTask;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.compiler.CompileContext;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * This extension is useful when there is a need to inject before and after actions with respect to the project task runner
 */
// consulo: upstream declares it in BazelProjectTaskRunner.kt; own file to keep the FQN. IntelliJ's ProjectTaskContext is the
// Consulo CompileContext of the build
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AdditionalProjectTaskRunnerProvider {
    @Nullable AdditionalProjectTask createTask(
        Project project,
        CompileContext projectTaskContext,
        List<Label> targetsToBuild
    );

    static List<AdditionalProjectTaskRunnerProvider> getEp() {
        return Application.get().getExtensionList(AdditionalProjectTaskRunnerProvider.class);
    }
}
