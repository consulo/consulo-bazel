package org.jetbrains.bazel.clion.debug;

import consulo.application.Application;
import consulo.application.progress.Task;
import consulo.fileEditor.FileEditorManager;
import consulo.language.file.light.ReadOnlyLightVirtualFile;
import consulo.language.plain.PlainTextLanguage;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Base class for an action that reports a part of the plugin state.
 *
 * <p>The action renders the value that {@link #exec} returns as JSON. It writes the JSON
 * to the log, and it opens it in a read-only editor tab when {@link #getShowOutputInEditor()}
 * is {@code true}. Call {@link #fail} to report a reason as an {@code error} field instead of an
 * exception.
 */
public abstract class BazelDebugAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private static final Logger LOG = Logger.getInstance(BazelDebugAction.class);

    private static final String BANNER = "#".repeat(80);

    // consulo: the action text comes from the constructor (upstream: the resource bundle of the XML registration)
    protected BazelDebugAction(LocalizeValue text) {
        super(text);
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)

    @Override
    public final void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabledAndVisible(project != null && org.jetbrains.bazel.config.project.isBazelProject(project));
    }

    @RequiredUIAccess
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        LocalizeValue text = e.getPresentation().getTextValue();
        LocalizeValue title = text.isEmpty() ? LocalizeValue.of(getClass().getSimpleName()) : text;

        // consulo: e.coroutineScope.launch(Dispatchers.Default) { withBackgroundProgress(project, title) { } }
        //   -> Task.Backgroundable.queue
        Task.Backgroundable.queue(project, title, true, indicator -> {
            try {
                report(project).join();
            }
            catch (CancellationException ignored) {
                // the coroutine was canceled, e.g. the popup was closed without a choice
            }
            catch (CompletionException ex) {
                if (!(ex.getCause() instanceof CancellationException)) {
                    throw ex;
                }
            }
        });
    }

    private CompletableFuture<?> report(Project project) {
        String name = getClass().getSimpleName();

        CompletableFuture<@Nullable Object> contentFuture;
        try {
            contentFuture = exec(project).thenApply(it -> it);
        }
        catch (DebugActionFailed ex) {
            contentFuture = CompletableFuture.completedFuture(Map.of("error", String.valueOf(ex.getMessage())));
        }

        // consulo: the try/catch of upstream also covers a failure of the future
        return contentFuture
            .exceptionally(error -> {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                if (cause instanceof DebugActionFailed ex) {
                    return Map.of("error", String.valueOf(ex.getMessage()));
                }
                throw error instanceof CompletionException completionException ? completionException : new CompletionException(error);
            })
            .thenCompose(content -> {
                // consulo: the local variable hides the file class json, which is written fully qualified
                String json = org.jetbrains.bazel.clion.debug.json.bazelDebugGson.toJson(content);

                LOG.info(BANNER);
                LOG.info(String.format("# %-76s #", name));
                LOG.info(BANNER);
                for (String line : json.lines().toList()) {
                    LOG.info(line);
                }
                LOG.info(BANNER);

                if (getShowOutputInEditor()) {
                    return openInEditor(project, name + ".json", json);
                }
                return CompletableFuture.<@Nullable Void>completedFuture(null);
            });
    }

    private CompletableFuture<@Nullable Void> openInEditor(
        Project project,
        String name,
        String text
    ) {
        // consulo: withContext(Dispatchers.EDT) -> UIAccess.giveAsync
        return project.getUIAccess().giveAsync(() -> {
            ReadOnlyLightVirtualFile file = new ReadOnlyLightVirtualFile(name, PlainTextLanguage.INSTANCE, text);
            FileEditorManager.getInstance(project).openFile(file, false);
            return Boolean.TRUE;
        }).<@Nullable Void>thenApply(ignored -> null);
    }

    protected CompletableFuture<WorkspaceSnapshot> snapshotOrFail(Project project) {
        return project.getInstance(WorkspaceSnapshotService.class).currentSnapshot().thenApply(snapshot -> {
            if (snapshot == WorkspaceSnapshot.EMPTY) {
                return fail("no workspace snapshot found, the project is not synced");
            }

            return snapshot;
        });
    }

    // consulo: Kotlin Nothing -> a generic return type, so the call can be used as an expression
    protected static <T> T fail(String reason) {
        throw new DebugActionFailed(reason);
    }

    protected boolean getShowOutputInEditor() {
        return Application.get().isInternal();
    }

    /**
     * Returns a tree of plain values. The base class renders it as pretty JSON.
     */
    protected abstract CompletableFuture<?> exec(Project project);
}

// consulo: unchecked like every Kotlin exception, so it can be thrown from the future callbacks
class DebugActionFailed extends RuntimeException {
    DebugActionFailed(String reason) {
        super(reason);
    }
}
