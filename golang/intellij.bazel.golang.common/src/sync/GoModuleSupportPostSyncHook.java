package org.jetbrains.bazel.golang.sync;

import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;

import java.util.concurrent.CompletableFuture;

// TODO consulo: not registered (upstream projectPostSyncHook). Consulo has neither VgoProjectSettings nor GoModuleSettings:
//  - the go.mod integration of consulo-google-go is an external system (consulo.go.modules) that is only active for linked go.mod
//    projects, there is no switch to turn it off;
//  - Go support of a module is its Go module extension, which GoWorkspaceImporter enables when it builds the workspace module
//    (the root model is rebuilt by every sync, so a post sync hook cannot keep it enabled)
public class GoModuleSupportPostSyncHook implements ProjectPostSyncHook {
    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHookEnvironment environment) {
        Project project = environment.project();

        // Bazel downloads dependencies on its own, so tracking go.mod files for changed dependencies doesn't make sense
        // TODO consulo: VgoProjectSettings.getInstance(project).isIntegrationEnabled = false
        // VgoProjectSettings.getInstance(project).isIntegrationEnabled = false

        Module workspaceModule = GoWorkspaceModuleUtil.findModule(project);
        if (workspaceModule == null) {
            return CompletableFuture.completedFuture(null);
        }
        // This should come after disabling go.mod integration, otherwise this will spam the UI with "go list"/"go mod download"
        // TODO consulo: GoModuleSettings.getInstance(workspaceModule).isGoSupportEnabled = true,
        //  see GoWorkspaceImporter.addGoWorkspaceModule
        // GoModuleSettings.getInstance(workspaceModule).isGoSupportEnabled = true
        return CompletableFuture.completedFuture(null);
    }
}
