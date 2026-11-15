package org.jetbrains.bazel.inspections;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

// consulo: upstream order="first", restricted to the plugin owning the extension point
@ExtensionImpl
public class JavaInspectionsSetup implements PostStartupActivity {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return;
        }

        // Switch off inspection "Java file is located outside of the module source root, so it won't be compiled"
        // Bazel defines its own source roots, and warns if source file does not belong to any target
        // TODO consulo: consulo-java has no "Java file outside of the module source root" check (JAVA_DONT_CHECK_OUT_OF_SOURCE_FILES)
        //PropertiesComponent.getInstance(project).setValue(JAVA_DONT_CHECK_OUT_OF_SOURCE_FILES, true)
    }
}
