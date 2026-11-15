package org.jetbrains.bazel.clion.workspace;

import consulo.application.Application;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.event.RunManagerListener;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream projectListeners for RunManagerListener, a @TopicImpl(ComponentScope.PROJECT) here),
//   OCWorkspace / OCWorkspaceModificationTrackersImpl.fireSelectedResolveConfigurationChanged (com.jetbrains.cidr.lang.workspace) do
//   not exist in consulo-cpp, which has no selected resolve configuration to refresh
public class CcRunConfigurationListener implements RunManagerListener {
    private final Project myProject;

    public CcRunConfigurationListener(Project project) {
        myProject = project;
    }

    @Override
    public void runConfigurationSelected(@Nullable RunnerAndConfigurationSettings settings) {
        updateResolveConfiguration();
    }

    @Override
    public void runConfigurationChanged(RunnerAndConfigurationSettings settings) {
        if (RunManager.getInstance(myProject).getSelectedConfiguration() == settings) {
            updateResolveConfiguration();
        }
    }

    private void updateResolveConfiguration() {
        if (!org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            return;
        }

        Application application = Application.get();
        application.invokeLater(() -> fireTrackerUpdates(application), myProject.getDisposed());
    }

    private void fireTrackerUpdates(Application application) {
        application.runWriteAction(() -> {
            // TODO consulo: OCWorkspace.getInstance(project).modificationTrackers does not exist in consulo-cpp
            // val trackers = OCWorkspace.getInstance(project).modificationTrackers as OCWorkspaceModificationTrackersImpl
            // trackers.fireSelectedResolveConfigurationChanged()
        });
    }
}
