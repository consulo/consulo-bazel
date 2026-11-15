package org.jetbrains.bazel.languages.starlark.bazel;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import java.util.List;

// consulo: upstream declares it in BazelGlobalFunctions.kt; own file to keep the FQN
@ExtensionAPI(ComponentScope.APPLICATION)
public interface StarlarkGlobalFunctionProvider {
    List<BazelGlobalFunctions.BazelGlobalFunction> functions(Project project);

    // consulo: upstream companion `extensionPoint` is Application.get().getExtensionPoint(StarlarkGlobalFunctionProvider.class)
}
