package org.jetbrains.bazel.ui.settings;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import org.jspecify.annotations.Nullable;

// TODO consulo: UpdateChecker, UpdateSettings.storedPluginHosts and IdeaPluginDescriptorImpl/PluginMainDescriptor
//  (JetBrains Marketplace plugin hosts) are missing, switching the update channel does not change the plugin repository
public final class BazelPluginUpdater {
    public static final String BAZEL_PLUGIN_ID = "org.jetbrains.bazel";

    private BazelPluginUpdater() {
    }

    public static void updatePlugins() {
        // TODO consulo: UpdateChecker is missing
        //UpdateChecker.updateAndShowResult(null, UpdateSettings.getInstance())
    }

    static void updatePluginsHost(UpdateChannel newUpdateChannel) {
        UpdateChannel currentUpdateChannel = BazelApplicationSettingsService.getInstance().getSettings().getUpdateChannel();
        updatePluginHost(newUpdateChannel, currentUpdateChannel);
    }

    private static void updatePluginHost(UpdateChannel newUpdateChannel, UpdateChannel currentUpdateChannel) {
        if (newUpdateChannel == currentUpdateChannel) {
            return;
        }
        // go from a presumably higher version (e.g., nightly) to a smaller version (e.g., release)
        if (newUpdateChannel.compareTo(currentUpdateChannel) < 0) {
            PluginDescriptor descriptor = getPluginDescriptorForId(BAZEL_PLUGIN_ID);
            if (descriptor != null) {
                patchPluginVersion("0.0.0", descriptor);
            }
        }
        // TODO consulo: UpdateSettings.storedPluginHosts is missing
        //val updateSettings = UpdateSettings.getInstance()
        //val nightlyRepo = UpdateChannel.NIGHTLY.getPluginUrlFromId(BAZEL_PLUGIN_ID)
        //updateSettings.storedPluginHosts.remove(nightlyRepo)
        //
        //when (newUpdateChannel) {
        //  UpdateChannel.NIGHTLY -> updateSettings.storedPluginHosts.add(nightlyRepo)
        //  else -> {}
        //}
    }

    /**
     * This function is used to force-downgrade a plugin version
     */
    public static void patchPluginVersion(String newVersion, PluginDescriptor descriptor) {
        // TODO consulo: PluginMainDescriptor (version field patched by reflection) is missing
        //if (descriptor !is PluginMainDescriptor)
        //  throw IllegalArgumentException("Unsupported descriptor type: ${descriptor.javaClass.name}")
        //
        //val versionField = PluginMainDescriptor::class.java.getDeclaredField("version")
        //try {
        //  versionField.setAccessible(true)
        //  versionField.set(descriptor, newVersion)
        //}
        //finally {
        //  versionField.setAccessible(false)
        //}
    }

    public static @Nullable PluginDescriptor getPluginDescriptorForId(String id) {
        return PluginManager.findPlugin(PluginId.getId(id));
    }
}
