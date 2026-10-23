package org.jetbrains.bazel.settings.bazel;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.buildifier.BuildifierUtil;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public record BazelProjectSettings(
    @Nullable Path buildifierExecutablePath,
    boolean runBuildifierOnSave
    // experimental settings
) {
    // consulo: upstream default arguments
    public BazelProjectSettings() {
        this(null, true);
    }

    public BazelProjectSettings withNewBuildifierExecutablePath(@Nullable Path newBuildifierExecutablePath) {
        return new BazelProjectSettings(newBuildifierExecutablePath, runBuildifierOnSave);
    }

    public @Nullable String getBuildifierPathString(Project project) {
        Path path = buildifierExecutablePath;
        if (path != null && Files.exists(path)) {
            return path.toAbsolutePath().toString();
        }
        return BuildifierUtil.detectBuildifierExecutable(project);
    }

    // consulo: upstream Kotlin extension property Project.bazelProjectSettings (declared at the end of the file).
    // Records are immutable, so upstream copy() calls are not needed.
    public static BazelProjectSettings getBazelProjectSettings(Project receiver) {
        return BazelProjectSettingsService.getInstance(receiver).getSettings();
    }

    public static void setBazelProjectSettings(Project receiver, BazelProjectSettings value) {
        BazelProjectSettingsService.getInstance(receiver).setSettings(value);
    }
}

class BazelProjectSettingsState {
    public @Nullable String buildifierExecutablePathUri = null;
    public boolean runBuildifierOnSave = true;

    public BazelProjectSettingsState() {
    }

    BazelProjectSettingsState(@Nullable String buildifierExecutablePathUri, boolean runBuildifierOnSave) {
        this.buildifierExecutablePathUri = buildifierExecutablePathUri;
        this.runBuildifierOnSave = runBuildifierOnSave;
    }

    public boolean isEmptyState() {
        return this.equals(new BazelProjectSettingsState());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BazelProjectSettingsState that = (BazelProjectSettingsState) o;
        return runBuildifierOnSave == that.runBuildifierOnSave &&
            Objects.equals(buildifierExecutablePathUri, that.buildifierExecutablePathUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildifierExecutablePathUri, runBuildifierOnSave);
    }

    @Override
    public String toString() {
        return "BazelProjectSettingsState(buildifierExecutablePathUri=" + buildifierExecutablePathUri +
            ", runBuildifierOnSave=" + runBuildifierOnSave + ")";
    }
}

@State(
    name = "BazelProjectSettingsService",
    storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
class BazelProjectSettingsService implements DumbAware, PersistentStateComponent<BazelProjectSettingsState> {
    private final Project myProject;
    private BazelProjectSettings mySettings = new BazelProjectSettings();

    @Inject
    BazelProjectSettingsService(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    public BazelProjectSettings getSettings() {
        return mySettings;
    }

    public void setSettings(BazelProjectSettings settings) {
        mySettings = settings;
    }

    @Override
    public BazelProjectSettingsState getState() {
        Path buildifierExecutablePath = mySettings.buildifierExecutablePath();
        return new BazelProjectSettingsState(
            buildifierExecutablePath != null ? buildifierExecutablePath.toUri().toString() : null,
            mySettings.runBuildifierOnSave()
        );
    }

    @Override
    public void loadState(BazelProjectSettingsState settingsState) {
        if (!settingsState.isEmptyState()) {
            String buildifierExecutablePathUri = settingsState.buildifierExecutablePathUri;
            mySettings =
                new BazelProjectSettings(
                    buildifierExecutablePathUri != null && !buildifierExecutablePathUri.isBlank()
                        ? Paths.get(URI.create(buildifierExecutablePathUri))
                        : null,
                    settingsState.runBuildifierOnSave
                );
        }
    }

    public static BazelProjectSettingsService getInstance(Project project) {
        return project.getInstance(BazelProjectSettingsService.class);
    }
}
