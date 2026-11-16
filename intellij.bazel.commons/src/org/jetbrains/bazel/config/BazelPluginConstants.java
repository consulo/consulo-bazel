package org.jetbrains.bazel.config;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.commons.constants.Constants;

public final class BazelPluginConstants {
    private BazelPluginConstants() {
    }

    public static final String BAZEL_TOOLWINDOW_ID = Constants.BAZEL_DISPLAY_NAME;

    /**
     * This id used in {@code BazelMoveDeclarationsToFileRefactoringListener}. Update the usage there if changing the constant
     */
    public static final String ID = "bazel";
    public static final String BAZEL_DISPLAY_NAME = BAZEL_TOOLWINDOW_ID;
    // consulo: the Consulo ProjectSystemId takes the display name as LocalizeValue
    public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(ID, LocalizeValue.of(BAZEL_DISPLAY_NAME));
    public static final String SE_LABEL_PROVIDER_ID = "LabelSearchEverywhereContributor";
}
