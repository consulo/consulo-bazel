package org.jetbrains.bazel.run.state;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.ui.ex.awt.JBCheckBox;

// consulo: upstream Kotlin top-level declarations of Android.kt; the interface is nested here
public final class Android {
    private Android() {
    }

    public interface HasUseMobileInstall {
        boolean getUseMobileInstall();

        void setUseMobileInstall(boolean useMobileInstall);
    }

    public static <T extends HasUseMobileInstall> SettingsEditorFragment<T, JBCheckBox> useMobileInstallFragment() {
        return new SettingsEditorFragment<T, JBCheckBox>(
            "useMobileInstall",
            BazelPluginLocalize.stateAndroidUseMobileInstallName(),
            null,
            new JBCheckBox(BazelPluginLocalize.stateAndroidUseMobileInstallName().get()),
            (settings, component) -> {
                component.setSelected(settings.getUseMobileInstall());
            },
            (settings, component) -> {
                settings.setUseMobileInstall(component.isSelected());
            },
            settings -> true
        );
    }
}
