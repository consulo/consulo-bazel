package org.jetbrains.bazel.languages.starlark.bazel.bzlmod;

import consulo.annotation.component.ActionImpl;
import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import org.jetbrains.bazel.action.SuspendableAction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@ActionImpl(id = "Bazel.RefreshBcrModules")
public class RefreshBcrModulesAction extends SuspendableAction {
    public RefreshBcrModulesAction() {
        super(StarlarkLocalize.bzlmodActionRefreshModules());
    }

    @Override
    protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
        // consulo: Kotlin runCatching { } over the suspend call
        CompletableFuture<?> refresh;
        try {
            refresh = BazelModuleRegistryService.getInstance(project).refreshModuleNames();
        }
        catch (Throwable t) {
            refresh = CompletableFuture.failedFuture(t);
        }
        return refresh.handle((ignored, error) -> {
            if (error == null) {
                notify(
                    project,
                    StarlarkLocalize.bzlmodNotificationBcrTitle(),
                    StarlarkLocalize.bzlmodNotificationBcrRefreshSuccess(),
                    NotificationType.INFORMATION
                );
            }
            else {
                Throwable throwable = unwrap(error);
                String message = throwable.getMessage();
                notify(
                    project,
                    StarlarkLocalize.bzlmodNotificationBcrTitle(),
                    StarlarkLocalize.bzlmodNotificationBcrRefreshFailure(message != null ? message : ""),
                    NotificationType.WARNING
                );
            }
            return null;
        });
    }

    private void notify(
        Project project,
        LocalizeValue title,
        LocalizeValue content,
        NotificationType type
    ) {
        // consulo: NotificationGroupManager.getNotificationGroup("BazelPlugin") is BazelNotificationGroupContributor.BAZEL_PLUGIN
        NotificationService.getInstance()
            .newOfType(BazelNotificationGroupContributor.BAZEL_PLUGIN, type)
            .title(title)
            .content(content)
            .notify(project);
    }

    // consulo: unwraps the CompletableFuture exception to the exception runCatching would see
    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
