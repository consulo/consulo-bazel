package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

// consulo: upstream class of BazelApplicationSettingsPanel.kt, own file because it is public and a @ServiceImpl
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
@State(
    name = "BazelApplicationSettingsService",
    storages = {
        @Storage(
            value = "bazel.application.settings.xml",
            roamingType = RoamingType.DISABLED
        ),
    }
)
public class BazelApplicationSettingsService implements PersistentStateComponent<BazelApplicationSettings> {
    private BazelApplicationSettings mySettings = new BazelApplicationSettings();

    public BazelApplicationSettings getSettings() {
        return mySettings;
    }

    public void setSettings(BazelApplicationSettings settings) {
        mySettings = settings;
    }

    @Override
    public BazelApplicationSettings getState() {
        return mySettings;
    }

    @Override
    public void loadState(BazelApplicationSettings state) {
        mySettings = state;
    }

    public static BazelApplicationSettingsService getInstance() {
        return Application.get().getInstance(BazelApplicationSettingsService.class);
    }
}
