package org.jetbrains.bazel.ui.settings;

public enum UpdateChannel {
    /**
     * If we don't override plugin repositories with custom URLs,
     * the IDEA will automatically use the default channel for retrieving plugin URLs.
     * The entry {@link #RELEASE} is here for settings and completeness and does not need the marketplace URLs.
     */
    RELEASE("Release", ""),
    NIGHTLY(
        "Nightly",
        Constants.BAZEL_PLUGIN_MARKETPLACE_URL.formatted(Constants.NIGHTLY_MARKETPLACE_CHANNEL)
    ),
    ;

    private final String myDisplayName;
    private final String myBazelPluginUrl;

    UpdateChannel(String displayName, String bazelPluginUrl) {
        myDisplayName = displayName;
        myBazelPluginUrl = bazelPluginUrl;
    }

    public String getDisplayName() {
        return myDisplayName;
    }

    public String getBazelPluginUrl() {
        return myBazelPluginUrl;
    }

    public String getPluginUrlFromId(String id) {
        if (BazelPluginUpdater.BAZEL_PLUGIN_ID.equals(id)) {
            return myBazelPluginUrl;
        }
        else {
            return "";
        }
    }

    // consulo: upstream private top-level constants of UpdateChannel.kt (enum constants cannot reference static fields of the enum)
    private static final class Constants {
        private static final String BAZEL_PLUGIN_MARKETPLACE_ID = "22977";

        private static final String BASE_PLUGIN_MARKETPLACE_URL = "https://plugins.jetbrains.com/plugins/%s/";
        private static final String BAZEL_PLUGIN_MARKETPLACE_URL = BASE_PLUGIN_MARKETPLACE_URL + BAZEL_PLUGIN_MARKETPLACE_ID;

        private static final String NIGHTLY_MARKETPLACE_CHANNEL = "nightly";
    }
}
