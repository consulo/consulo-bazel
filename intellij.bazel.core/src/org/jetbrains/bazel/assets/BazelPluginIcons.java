package org.jetbrains.bazel.assets;

import consulo.bazel.core.icon.BazelPluginIconGroup;
import consulo.ui.image.Image;

// TODO: this should be generated automatically
// consulo: delegates to the generated consulo.bazel.core.icon.BazelPluginIconGroup (ICON-LIB)
@SuppressWarnings("unused") // should be able to provide us with all available icons
public final class BazelPluginIcons {
    private BazelPluginIcons() {
    }

    public static Image bazel() {
        return BazelPluginIconGroup.bazel();
    }

    public static Image bazelConfig() {
        return BazelPluginIconGroup.bazelconfig();
    }

    public static Image bazelDirectory() {
        return BazelPluginIconGroup.bazeldirectory();
    }

    public static Image bazelError() {
        return BazelPluginIconGroup.bazelerror();
    }

    public static Image bazelReload() {
        return BazelPluginIconGroup.bazelreload();
    }

    public static Image bazelToolWindow() {
        return BazelPluginIconGroup.toolwindowbazel();
    }

    public static Image bazelWarning() {
        return BazelPluginIconGroup.bazelwarning();
    }

    // consulo: upstream references these two by path in intellij.bazel.core.xml (icon="/icons/resync.svg", "/icons/buildAndResync.svg")
    public static Image resync() {
        return BazelPluginIconGroup.resync();
    }

    public static Image buildAndResync() {
        return BazelPluginIconGroup.buildandresync();
    }
}
