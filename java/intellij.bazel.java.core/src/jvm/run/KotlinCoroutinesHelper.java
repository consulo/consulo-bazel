package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.project.Project;

import java.util.List;

// TODO: remove this, it's only being used to get rid of
//   `intellij.kotlin.jvm.debugger.coroutines` dependency from `bazel.java.common`
@ExtensionAPI(ComponentScope.APPLICATION)
public interface KotlinCoroutinesHelper {
    void attachCoroutinesDebuggerConnection(Project project, RunConfigurationBase runConfiguration);

    // consulo: upstream `ep`, served by the Consulo extension point
    static List<KotlinCoroutinesHelper> getEp() {
        return Application.get().getExtensionList(KotlinCoroutinesHelper.class);
    }
}
