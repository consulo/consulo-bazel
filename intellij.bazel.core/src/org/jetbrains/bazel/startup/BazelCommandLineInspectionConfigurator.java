package org.jetbrains.bazel.startup;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

// TODO consulo: CommandLineInspectionProjectConfigurator (<commandLineInspectionProjectConfigurator>) is missing in Consulo,
//  the class implements no platform interface and is not registered
class BazelCommandLineInspectionConfigurator {
    public String getName() {
        return "Bsp Command Line Inspection";
    }

    public LocalizeValue getDescription() {
        return BazelPluginLocalize.inspectionConfiguratorBazelCommandlineDescription();
    }

    // consulo: the upstream CommandLineInspectionProjectConfigurator.ConfiguratorContext parameter is dropped
    public void configureProject(Project project) {
        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            // TODO consulo: the ActivityTracker extension point (com.intellij.platform.backend.observation.EP_NAME) is missing
            // runBlocking {
            //   EP_NAME.extensionList
            //     .filterIsInstance<BazelStartupActivityTracker>()
            //     .firstOrNull()
            //     ?.awaitConfiguration(project)
            // }
        }
    }
}
