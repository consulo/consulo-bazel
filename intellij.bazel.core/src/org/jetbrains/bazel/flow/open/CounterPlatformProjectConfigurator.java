package org.jetbrains.bazel.flow.open;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.WriteAction;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;

import java.util.concurrent.CompletableFuture;

/**
 * Clean up any modules showing up due to the platform hack
 * https://youtrack.jetbrains.com/issue/IDEA-321160/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
 *
 * This is not needed in most of the cases, but there are still rare project configurators that
 * may create a module via {@code com.intellij.openapi.project.impl.CreateModuleKt.getOrInitializeModule}
 */
// TODO consulo: DirectoryProjectConfigurator is missing in Consulo; the class is not registered. The IntelliJ workspace model
// update is written with the classic module/library API, a module is a Bazel one when BazelProjectModelService owns it
public class CounterPlatformProjectConfigurator {
    public CompletableFuture<?> configure(Project project) {
        return configureProjectCounterPlatform(project);
    }

    public static CompletableFuture<?> configureProjectCounterPlatform(Project project) {
        // consulo: upstream also repeats the cleanup when JpsProjectLoadingManager reports the JPS project as loaded
        // (https://youtrack.jetbrains.com/issue/BAZEL-1933); Consulo loads the project model before the project opens
        return removeFakeModulesAndLibraries(project);
    }

    private static CompletableFuture<?> removeFakeModulesAndLibraries(Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return CompletableFuture.completedFuture(null);
        }

        return project.getUIAccess().giveAsync(() -> {
            WriteAction.run(() -> updateProjectModel(project));
        });
    }

    /**
     * Not using asynchronous {@code WorkspaceModel.update} here to be extra sure that scanning can't start in between
     */
    @RequiredWriteAction
    private static void updateProjectModel(Project project) {
        BazelProjectModelService modelService = BazelProjectModelService.getInstance(project);
        ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
        for (Module module : moduleModel.getModules()) {
            if (!modelService.isBazelModule(module)) {
                moduleModel.disposeModule(module);
            }
        }
        moduleModel.commit();

        LibraryTable.ModifiableModel libraryModel = ProjectLibraryTable.getInstance(project).getModifiableModel();
        for (Library library : libraryModel.getLibraries()) {
            libraryModel.removeLibrary(library);
        }
        libraryModel.commit();
    }
}
