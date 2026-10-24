package org.jetbrains.bazel.languages.projectview.base;

import consulo.bazel.projectview.localize.BazelProjectViewLocalize;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class ProjectViewFileType extends LanguageFileType {
    public static final ProjectViewFileType INSTANCE = new ProjectViewFileType();

    private ProjectViewFileType() {
        super(ProjectViewLanguage.INSTANCE);
    }

    // TODO: whole icon loading for entire bazel plugin should be moved to dedicated module
    //  this works because intellij.bazel.projectview module is in the same classloader as core module
    // consulo: upstream loads icons/bazel.svg with IconLoader; the icon is served by BazelLanguagesIconGroup
    private final Image myBazelIcon = BazelLanguagesIcons.bazel();

    public Image getBazelIcon() {
        return myBazelIcon;
    }

    @Override
    public String getId() {
        return "ProjectView";
    }

    @Override
    public LocalizeValue getDescription() {
        return BazelProjectViewLocalize.bazelLanguageProjectviewDescription();
    }

    @Override
    public String getDefaultExtension() {
        return "bazelproject";
    }

    @Override
    public Image getIcon() {
        return myBazelIcon;
    }
}
