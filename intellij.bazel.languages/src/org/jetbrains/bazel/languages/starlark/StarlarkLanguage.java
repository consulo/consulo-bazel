package org.jetbrains.bazel.languages.starlark;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class StarlarkLanguage extends Language {
    public static final StarlarkLanguage INSTANCE = new StarlarkLanguage();

    private StarlarkLanguage() {
        super("Starlark");
    }

    // consulo: upstream declares this object as a top-level declaration in the same file
    public static class StarlarkFileType extends LanguageFileType {
        public static final StarlarkFileType INSTANCE = new StarlarkFileType();

        private StarlarkFileType() {
            super(StarlarkLanguage.INSTANCE);
        }

        @Override
        public String getId() {
            return "Starlark";
        }

        @Override
        public LocalizeValue getDescription() {
            return BazelLanguagesLocalize.starlarkLanguageDescription();
        }

        @Override
        public String getDefaultExtension() {
            return "bzl";
        }

        @Override
        public Image getIcon() {
            return BazelLanguagesIcons.bazel();
        }
    }
}
