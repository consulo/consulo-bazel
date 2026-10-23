package org.jetbrains.bazel.languages.bazelrc;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class BazelrcLanguage extends Language {
    public static final BazelrcLanguage INSTANCE = new BazelrcLanguage();

    private BazelrcLanguage() {
        super("Bazelrc");
    }

    public static class BazelrcFileType extends LanguageFileType {
        public static final BazelrcFileType INSTANCE = new BazelrcFileType();

        private BazelrcFileType() {
            super(BazelrcLanguage.INSTANCE);
        }

        @Override
        public String getId() {
            return "Bazelrc";
        }

        @Override
        public LocalizeValue getDescription() {
            return BazelLanguagesLocalize.bazelrcLanguageDescription();
        }

        @Override
        public String getDefaultExtension() {
            return "bazelrc";
        }

        @Override
        public Image getIcon() {
            return BazelLanguagesIcons.bazel();
        }
    }
}
