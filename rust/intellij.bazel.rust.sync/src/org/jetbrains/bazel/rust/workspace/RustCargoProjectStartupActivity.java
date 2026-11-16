package org.jetbrains.bazel.rust.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.bazel.startup.utils.BazelProjectActivity;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.model.ExternalCargoProjectData;

import java.util.concurrent.CompletableFuture;

/**
 * Attaches the Rust crates of the persisted workspace snapshot to consulo-rust when a Bazel project is opened.
 * <p>
 * consulo: Consulo-only. consulo-rust does not persist an external Cargo project (its build system attaches it again on every
 * open), and a sync is not guaranteed to run at startup: the crates are read from the workspace snapshot the last sync stored, the
 * same way {@link RustBazelWorkspaceImporter} reads them from the snapshot of a sync.
 */
@ExtensionImpl
public class RustCargoProjectStartupActivity extends BazelProjectActivity {
    private static final Logger LOG = Logger.getInstance(RustCargoProjectStartupActivity.class);

    @Override
    public CompletableFuture<?> executeForBazelProject(Project project) {
        return WorkspaceHelpers.currentWorkspaceSnapshotOrNull(project)
            .<@Nullable ExternalCargoProjectData>thenApplyAsync(snapshot -> {
                if (snapshot == null) {
                    return null;
                }
                BazelRustWorkspace workspace = BazelRustWorkspace.of(snapshot);
                return workspace.isEmpty() ? null : workspace.toExternalCargoProjectData();
            }, Application.get().getInstance(ApplicationConcurrency.class).executor())
            .thenCompose(data -> data == null || project.isDisposed()
                ? CompletableFuture.<Void>completedFuture(null)
                : RustBazelWorkspaceImporter.attach(project, data))
            .whenComplete((ignored, error) -> {
                if (error != null) {
                    LOG.warn("Cannot attach the Bazel Rust workspace", error);
                }
            });
    }
}
