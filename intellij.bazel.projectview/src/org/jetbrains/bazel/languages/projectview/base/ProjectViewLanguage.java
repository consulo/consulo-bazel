package org.jetbrains.bazel.languages.projectview.base;

import consulo.language.Language;

public class ProjectViewLanguage extends Language {
    public static final ProjectViewLanguage INSTANCE = new ProjectViewLanguage();

    private ProjectViewLanguage() {
        super("ProjectView");
    }
}
