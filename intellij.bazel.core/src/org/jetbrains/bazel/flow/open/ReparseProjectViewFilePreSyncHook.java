package org.jetbrains.bazel.flow.open;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.MessageEvent;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.ProjectPreSyncHook;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class ReparseProjectViewFilePreSyncHook implements ProjectPreSyncHook {
    @Override
    public CompletableFuture<Void> onPreSync(ProjectPreSyncHook.ProjectPreSyncHookEnvironment environment) {
        Project project = environment.project();
        return ProjectPreSyncHook.withSubtask(environment, BazelPluginLocalize.projectViewValidateSubtask(), taskId -> {
            ProjectViewService projectViewService = ProjectViewService.getInstance(project);
            return projectViewService.forceReparseCurrentProjectViewFiles().thenApply(unused -> {
                List<Import.Unresolved> unresolvedRequiredImports = ProjectView.unresolvedRequiredImports(projectViewService.getProjectView());
                for (Import.Unresolved it : unresolvedRequiredImports) {
                    @Nullable FilePosition position = it.position();
                    util.getSyncConsole(project).addDiagnosticMessage(
                        taskId,
                        position == null ? null : position.getFile().toPath(),
                        position == null ? -1 : position.getStartLine(),
                        position == null ? -1 : position.getStartColumn(),
                        BazelPluginLocalize.projectViewImportUnresolved(it.text()),
                        null,
                        MessageEvent.Kind.ERROR
                    );
                }
                return null;
            });
        });
    }
}
