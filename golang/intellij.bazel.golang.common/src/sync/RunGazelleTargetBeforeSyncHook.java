package org.jetbrains.bazel.golang.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.build.ui.event.MessageEvent;
import consulo.bazel.go.localize.BazelGoLocalize;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.sync.ProjectPreSyncHook;
import org.jetbrains.bsp.protocol.RunParams;

import java.util.concurrent.CompletableFuture;

// consulo: holder of the upstream top-level declarations of RunGazelleTargetBeforeSyncHook.kt; the hook itself is
// RunGazelleTargetPreSyncHook below
public final class RunGazelleTargetBeforeSyncHook {
    private RunGazelleTargetBeforeSyncHook() {
    }

    public static final Key<Label> GAZELLE_TARGET_OVERRIDE_KEY = Key.create("GAZELLE_TARGET_OVERRIDE_KEY");
}

@ExtensionImpl
class RunGazelleTargetPreSyncHook implements ProjectPreSyncHook {
    public RunGazelleTargetPreSyncHook() {
    }

    @Override
    public CompletableFuture<Void> onPreSync(ProjectPreSyncHookEnvironment environment) {
        Project project = environment.project();
        Label gazelleTarget = project.getUserData(RunGazelleTargetBeforeSyncHook.GAZELLE_TARGET_OVERRIDE_KEY);
        if (gazelleTarget == null) {
            gazelleTarget = ProjectViewExtensions.getGazelleTarget(ProjectViewService.projectView(environment.project()));
        }
        if (gazelleTarget == null) {
            return CompletableFuture.completedFuture(null);
        }
        Label target = gazelleTarget;
        // TODO consulo: the upstream text is not localized, add a BazelGoLocalize key for "Run Gazelle target"
        return ProjectPreSyncHook.withSubtask(environment, BazelGoLocalize.syncGazelleRunTarget(), taskId ->
            BazelServerConnection.getConnection(project).runWithServer(taskId, server -> {
                RunParams runParams =
                    new RunParams(
                        taskId,
                        target,
                        true
                    );
                return server.buildTargetRun(runParams).thenApply(buildTargetRun -> {
                    if (buildTargetRun.statusCode() == BazelStatus.BUILD_ERROR) {
                        util.getSyncConsole(project).addDiagnosticMessage(
                            taskId,
                            null,
                            -1,
                            -1,
                            // TODO consulo: the upstream text is not localized, add a BazelGoLocalize key
                            BazelGoLocalize.syncGazelleTargetHint(),
                            null,
                            MessageEvent.Kind.WARNING
                        );
                    }
                    return null;
                });
            })
        );
    }
}
