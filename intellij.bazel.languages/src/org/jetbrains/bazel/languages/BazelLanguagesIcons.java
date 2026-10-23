package org.jetbrains.bazel.languages;

import consulo.bazel.languages.icon.BazelLanguagesIconGroup;
import consulo.ui.image.Image;

/**
 * Icons of the Bazel languages. The icon files live in `intellij.bazel.resource`.
 */
public final class BazelLanguagesIcons {
    private BazelLanguagesIcons() {
    }

    public static Image bazel() {
        return BazelLanguagesIconGroup.bazel();
    }
}
