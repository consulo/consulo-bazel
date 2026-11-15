package org.jetbrains.bazel.ui.status;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.sync.status.SyncStatusListener;
import org.jetbrains.bazel.target.TargetStorage;

// consulo: upstream class of BazelFileStatusDecorator.kt, own file because it is public and a @ServiceImpl
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelFileStatusRefresher {
    private final Project myProject;

    @Inject
    public BazelFileStatusRefresher(Project project) {
        myProject = project;

        // consulo: upstream collects targetStorage.views (StateFlow) in the service coroutine scope;
        // collect() also handles the current value, so refresh once and then on every change
        TargetStorage.getTargetStorage(project).getViews().addListener(it -> refreshAllFilesPresentation());
        refreshAllFilesPresentation();

        project.getMessageBus().connect().subscribe(
            SyncStatusListener.class,
            new SyncStatusListener() {
                @Override
                public void syncStarted() {
                    // Cleanup all "unsynced" presentations early
                    refreshAllFilesPresentation();
                }

                @Override
                public void syncFinished(boolean canceled) {
                    refreshAllFilesPresentation();
                }
            }
        );
    }

    public void refreshAllFilesPresentation() {
        BazelCoroutineService.getInstance(myProject).start(() ->
            myProject.getUIAccess().giveAsync(() -> {
                // consulo: ProjectViewUpdateCause is not ported
                ProjectView.getInstance(myProject).refresh();
                EditorNotifications.getInstance(myProject).updateAllNotifications();
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
                for (VirtualFile it : fileEditorManager.getOpenFiles()) {
                    fileEditorManager.updateFilePresentation(it);
                }
            })
        );
    }

    public static BazelFileStatusRefresher getInstance(Project project) {
        return project.getInstance(BazelFileStatusRefresher.class);
    }
}
