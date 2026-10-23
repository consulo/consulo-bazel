package org.jetbrains.bazel.languages.bazelversion;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class BazelVersionLanguage extends Language {
    public static final BazelVersionLanguage INSTANCE = new BazelVersionLanguage();

    private BazelVersionLanguage() {
        super("BazelVersion");
    }

    public static class BazelVersionFileType extends LanguageFileType {
        public static final BazelVersionFileType INSTANCE = new BazelVersionFileType();

        private BazelVersionFileType() {
            super(BazelVersionLanguage.INSTANCE);
        }

        @Override
        public String getId() {
            return "BazelVersion";
        }

        @Override
        public LocalizeValue getDescription() {
            return BazelLanguagesLocalize.bazelLanguageDescription();
        }

        @Override
        public String getDefaultExtension() {
            return "bazelversion";
        }

        @Override
        public Image getIcon() {
            return BazelLanguagesIcons.bazel();
        }
    }
}
