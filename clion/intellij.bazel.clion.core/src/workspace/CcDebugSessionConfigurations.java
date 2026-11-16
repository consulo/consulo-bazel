package org.jetbrains.bazel.clion.workspace;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Holds the configurations of the build of the last debug session. {@link CcDebugSessionChooser} selects a resolve configuration
 * that has one of them.
 * <p>
 * The state lives as long as the project is open. A sync does not clear it, because a configuration that the new snapshot
 * does not contain matches no resolve configuration.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class CcDebugSessionConfigurations {
    public static CcDebugSessionConfigurations getInstance(Project project) {
        return project.getInstance(CcDebugSessionConfigurations.class);
    }

    /** The configuration of the debugged target comes first, the configurations of its dependencies follow. */
    private volatile List<WorkspaceConfigurationId> myConfigurations = List.of();

    public List<WorkspaceConfigurationId> getConfigurations() {
        return myConfigurations;
    }

    /** Stores the configurations of a new debug session and tells the resolution engine to select the resolve configurations again. */
    // consulo: upstream suspend fun with edtWriteAction; the store needs no write action here
    public synchronized void update(List<WorkspaceConfigurationId> newConfigurations) {
        List<WorkspaceConfigurationId> distinct = new ArrayList<>(new LinkedHashSet<>(newConfigurations));
        if (distinct.equals(myConfigurations)) {
            return;
        }
        myConfigurations = List.copyOf(distinct);
        // TODO consulo: upstream fires OCWorkspaceModificationTrackersImpl.fireSelectedResolveConfigurationChanged(); consulo-cpp has
        //  no way to refresh the file options outside a project model commit, so the new choice is picked up on the next one
    }
}
