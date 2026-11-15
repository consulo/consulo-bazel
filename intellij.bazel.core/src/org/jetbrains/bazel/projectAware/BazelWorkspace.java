package org.jetbrains.bazel.projectAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.sync.ProjectDirtyStateService;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusListener;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelWorkspace implements ExternalSystemProjectAware, Disposable {
    private final Project myProject;

    private boolean myInitialized = false;

    private volatile boolean myDisposed = false;

    // computed eagerly: the tracker's remove callback reads this during disposal, when service access already throws
    private final ExternalSystemProjectId myProjectId;

    @Inject
    public BazelWorkspace(Project project) {
        myProject = project;
        myProjectId = getBazelProjectId(org.jetbrains.bazel.config.project.getRootDir(project));
    }

    public Project getProject() {
        return myProject;
    }

    @Override
    public ExternalSystemProjectId getProjectId() {
        return myProjectId;
    }

    @Override
    public Set<String> getSettingsFiles() {
        return Set.of();
    }

    /**
     * Project disposal runs inside a write action, so performing the registration under a read action
     * guarantees it cannot interleave with the disposal of this workspace or of the auto-import tracker (BAZEL-3453).
     */
    public CompletableFuture<?> initialize() {
        // consulo: upstream suspending readAction { }, runs as a blocking read action in the calling thread
        ReadAction.run(() -> {
            if (myInitialized || myDisposed || myProject.isDisposed()) {
                return;
            }
            ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(myProject);
            projectTracker.register(this, /* parentDisposable = */ this);
            projectTracker.activate(myProjectId);
            new BspExternalServicesSubscriber(myProject).subscribe(this);
            myInitialized = true;
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void reloadProject(ExternalSystemProjectReloadContext context) {
        if (context.isExplicitReload()) {
            // TODO: use context after https://youtrack.jetbrains.com/issue/BAZEL-3570
            ProjectDirtyStateService dirtyStateService = ProjectDirtyStateService.getInstance(myProject);
            BazelCoroutineService.getInstance(myProject).start(() -> {
                ProjectDirtyStateService.ProjectDirtyState dirtyState = dirtyStateService.current();
                ProjectSyncScope scope;
                if (dirtyState.isEmpty() /* Generally should not happen */ || dirtyState.wholeProject()) {
                    scope = new ProjectSyncScope.Full(false, false);
                }
                else {
                    scope = new ProjectSyncScope.Files(new ArrayList<>(dirtyState.paths()), false);
                }
                return myProject.getInstance(ProjectSyncService.class).sync(scope);
            });
        }
    }

    /**
     * Tells the platform to ignore every settings file event.
     * The plugin tracks the Bazel files itself. See {@code org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor}.
     */
    @Override
    public boolean isIgnoredSettingsFileEvent(String path, ExternalSystemSettingsFilesModificationContext context) {
        return true;
    }

    @Override
    public void subscribe(ExternalSystemProjectListener listener, Disposable parentDisposable) {
        myProject.getMessageBus().connect(parentDisposable).subscribe(
            SyncStatusListener.TOPIC,
            new SyncStatusListener() {
                @Override
                public void syncStarted() {
                    listener.onProjectReloadStart();
                }

                @Override
                public void syncFinished(boolean canceled) {
                    ProjectViewUtil.expandTopLevel(myProject);
                    listener.onProjectReloadFinish(
                        canceled ? ExternalSystemRefreshStatus.CANCEL : ExternalSystemRefreshStatus.SUCCESS
                    );
                }
            }
        );
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    public static BazelWorkspace getInstance(Project project) {
        return project.getInstance(BazelWorkspace.class);
    }

    private static ExternalSystemProjectId getBazelProjectId(VirtualFile projectPath) {
        return new ExternalSystemProjectId(BazelPluginConstants.SYSTEM_ID, projectPath.getPath());
    }
}

class BspExternalServicesSubscriber {
    private final Project myProject;

    BspExternalServicesSubscriber(Project project) {
        myProject = project;
    }

    void subscribe(Disposable parentDisposable) {
        subscribeForBranchChanges(parentDisposable);
    }


    private void subscribeForBranchChanges(Disposable parentDisposable) {
        // TODO consulo: BranchChangeListener (VCS_BRANCH_CHANGED topic) is missing in Consulo
        // project.messageBus.connect(parentDisposable).subscribe(
        //   BranchChangeListener.VCS_BRANCH_CHANGED,
        //   object : BranchChangeListener {
        //     override fun branchWillChange(branchName: String) {}
        //
        //     override fun branchHasChanged(branchName: String) {
        //       // automatically expand the project view tree's root node on branch change event
        //       // TODO: remove this workaround once this bug is fixed from the platform side
        //       // https://youtrack.jetbrains.com/issue/IJPL-160019/Restore-workspace-when-switching-branches-collapses-root-in-project-tree-view-when-switching-between-local-branches
        //       ProjectViewUtil.expandTopLevel(project)
        //     }
        //   },
        // )
    }
}

final class ProjectViewUtil {
    private ProjectViewUtil() {
    }

    static void expandTopLevel(Project project) {
        // TODO consulo: the tree of the current project view pane is not exposed by the Consulo API
        //  (consulo.project.ui.view.ProjectViewPane has no getTree(), AbstractProjectViewPane is in ide-impl)
        // val projectView = ProjectView.getInstance(project)
        //
        // // this visitor will traverse first the main project root, which has 2 elements in its path,
        // // therefore, we stop right after it finishes visiting this project root
        // val rootNoteExpandVisitor =
        //   TreeVisitor { treePath ->
        //     if (treePath.pathCount == 1) {
        //       TreeVisitor.Action.CONTINUE
        //     } else {
        //       TreeVisitor.Action.INTERRUPT
        //     }
        //   }
        // val pane = projectView.currentProjectViewPane ?: return
        // TreeUtil.expand(pane.tree, rootNoteExpandVisitor) {}
    }
}
