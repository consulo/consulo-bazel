package org.jetbrains.bazel.languages.projectview.formatting;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;

public class ProjectViewCodeStyleSettings extends CustomCodeStyleSettings {
    public ProjectViewCodeStyleSettings(CodeStyleSettings settings) {
        super("ProjectView", settings);
    }
}
