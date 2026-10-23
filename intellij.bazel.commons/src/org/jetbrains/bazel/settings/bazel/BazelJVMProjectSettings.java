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
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * BazelJVMProjectSettings holds the JVM-specific part of settings. It can be modified by BazelJVMSettingsProviders,
 * which injects JVM-specific settings into the Bazel setting panel. However, unlike BazelJVMSettingsProviders, this BazelJVMProjectSettings is always available
 * even there is no java-plugin.
 * * (BazelJVMSettingsProviders is the extension of an extension point, and it will not be loaded when java plugin is absent, so that user won't be able to change t
 * the JVM-specific settings when there is no java plugin)
 */
public record BazelJVMProjectSettings(
    boolean hotSwapEnabled,
    boolean enableKotlinCoroutineDebug
) {
    // consulo: upstream default arguments
    public BazelJVMProjectSettings() {
        this(true, true);
    }

    // consulo: upstream Kotlin extension property Project.bazelJVMProjectSettings (declared at the end of the file)
    public static BazelJVMProjectSettings getBazelJVMProjectSettings(Project receiver) {
        BazelJVMProjectSettingsState settings = BazelJVMProjectSettingsService.getInstance(receiver).getSettings();
        return new BazelJVMProjectSettings(
            settings.hotSwapEnabled,
            settings.enableKotlinCoroutineDebugV2
        );
    }

    public static void setBazelJVMProjectSettings(Project receiver, BazelJVMProjectSettings value) {
        BazelJVMProjectSettingsService.getInstance(receiver).setSettings(
            new BazelJVMProjectSettingsState(
                value.hotSwapEnabled(),
                value.enableKotlinCoroutineDebug()
            )
        );
    }
}

@State(
    name = "BazelJVMProjectSettingsService",
    storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
class BazelJVMProjectSettingsService implements DumbAware, PersistentStateComponent<BazelJVMProjectSettingsState> {
    private BazelJVMProjectSettingsState mySettings = new BazelJVMProjectSettingsState();

    @Inject
    BazelJVMProjectSettingsService() {
    }

    public BazelJVMProjectSettingsState getSettings() {
        return mySettings;
    }

    public void setSettings(BazelJVMProjectSettingsState settings) {
        mySettings = settings;
    }

    @Override
    public BazelJVMProjectSettingsState getState() {
        return mySettings;
    }

    @Override
    public void loadState(BazelJVMProjectSettingsState settingsState) {
        mySettings = settingsState;
    }

    public static BazelJVMProjectSettingsService getInstance(Project project) {
        return project.getInstance(BazelJVMProjectSettingsService.class);
    }
}

/**
 * State for XML serialization
 */
class BazelJVMProjectSettingsState {
    public boolean hotSwapEnabled = true;
    public boolean enableKotlinCoroutineDebugV2 = true;  // Default was changed from false to true, "v2" to avoid loading the old key

    public BazelJVMProjectSettingsState() {
    }

    BazelJVMProjectSettingsState(boolean hotSwapEnabled, boolean enableKotlinCoroutineDebugV2) {
        this.hotSwapEnabled = hotSwapEnabled;
        this.enableKotlinCoroutineDebugV2 = enableKotlinCoroutineDebugV2;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BazelJVMProjectSettingsState that = (BazelJVMProjectSettingsState) o;
        return hotSwapEnabled == that.hotSwapEnabled && enableKotlinCoroutineDebugV2 == that.enableKotlinCoroutineDebugV2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotSwapEnabled, enableKotlinCoroutineDebugV2);
    }

    @Override
    public String toString() {
        return "BazelJVMProjectSettingsState(hotSwapEnabled=" + hotSwapEnabled +
            ", enableKotlinCoroutineDebugV2=" + enableKotlinCoroutineDebugV2 + ")";
    }
}
