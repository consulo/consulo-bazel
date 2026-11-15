package org.jetbrains.bazel.ui.notifications;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.component.extension.ExtensionPoint;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.content.ProjectFileIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.sync.ProjectPostSyncHook;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@ExtensionImpl
public class BuildAndResyncOnUnresolvedImportNotificationsProvider implements
    EditorNotificationProvider,
    DumbAware {
    private final Project myProject;

    private final Set<VirtualFile> myDisableNotificationForFile = new HashSet<>();

    @Inject
    public BuildAndResyncOnUnresolvedImportNotificationsProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "BuildAndResyncOnUnresolvedImportNotificationsProvider";
    }

    // consulo: upstream collectNotificationData(project, file) returning a panel factory
    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(
        VirtualFile file,
        FileEditor fileEditor,
        Supplier<EditorNotificationBuilder> builderFactory
    ) {
        Project project = myProject;
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        if (myDisableNotificationForFile.contains(file)) {
            return null;
        }
        if (SyncStatusService.isSyncInProgress(project)) {
            return null;
        }
        if (BazelFeatureFlags.isBuildProjectOnSyncEnabled()) {
            return null;
        }
        // consulo: IncompleteDependenciesService is not ported, the dependencies state is always complete
        //if (!project.service<IncompleteDependenciesService>().getState().isComplete) return null

        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
            dumbService.runWhenSmart(() -> EditorNotifications.getInstance(project).updateAllNotifications());
            return null;
        }

        if (!hasUnresolvedImport(project, file)) {
            return null;
        }

        return buildAndResyncOnUnresolvedImportEditorPanel(project, fileEditor, builderFactory.get());
    }

    @RequiredReadAction
    private boolean hasUnresolvedImport(Project project, VirtualFile file) {
        // TODO Scala
        if (!isKotlinFileType(file) && !isJavaFileType(file)) {
            return false;
        }
        if (!ProjectFileIndex.getInstance(project).isInSourceContent(file)) {
            return false;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }

        return UnresolvedImportChecker.getEp().getExtensionList().stream().anyMatch(it -> it.hasUnresolvedImport(project, psiFile));
    }

    private boolean isKotlinFileType(VirtualFile receiver) {
        return Objects.equals(receiver.getExtension(), "kt");
    }

    private boolean isJavaFileType(VirtualFile receiver) {
        return Objects.equals(receiver.getExtension(), "java");
    }

    // consulo: upstream inner class BuildAndResyncOnUnresolvedImportEditorPanel (EditorNotificationPanel) fills the Consulo builder
    private EditorNotificationBuilder buildAndResyncOnUnresolvedImportEditorPanel(
        Project project,
        FileEditor fileEditor,
        EditorNotificationBuilder builder
    ) {
        builder.asWarning();
        builder.withText(BazelPluginLocalize.notificationUnresolvedImports());

        builder.withAction(
            BazelPluginLocalize.buildAndResyncActionText(),
            e -> BazelCoroutineService.getInstance(project).start(() ->
                project.getInstance(ProjectSyncService.class).sync(new ProjectSyncScope.Full(true, false))
            )
        );
        // TODO consulo: EditorNotificationBuilder has no close action
        //val virtualFile = fileEditor.file
        //setCloseAction {
        //  disableNotificationForFile.add(virtualFile)
        //  EditorNotifications.getInstance(project).updateAllNotifications()
        //}
        return builder;
    }

    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface UnresolvedImportChecker {
        boolean hasUnresolvedImport(Project project, PsiFile psiFile);

        static ExtensionPoint<UnresolvedImportChecker> getEp() {
            return Application.get().getExtensionPoint(UnresolvedImportChecker.class);
        }
    }
}

// consulo: upstream nested class BuildAndResyncOnUnresolvedImportNotificationsProvider.NotificationUpdateSyncHook
// (nested extension implementations are not supported by the annotation processor)
@ExtensionImpl
class NotificationUpdateSyncHook implements ProjectPostSyncHook {
    public NotificationUpdateSyncHook() {
    }

    @Override
    public CompletableFuture<Void> onPostSync(ProjectPostSyncHook.ProjectPostSyncHookEnvironment environment) {
        EditorNotifications.getInstance(environment.project()).updateAllNotifications();
        return CompletableFuture.completedFuture(null);
    }
}
