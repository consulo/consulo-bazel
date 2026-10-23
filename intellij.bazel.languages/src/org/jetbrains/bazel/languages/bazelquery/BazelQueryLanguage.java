package org.jetbrains.bazel.languages.bazelquery;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class BazelQueryLanguage extends Language {
    public static final BazelQueryLanguage INSTANCE = new BazelQueryLanguage();

    private BazelQueryLanguage() {
        super("BazelQuery");
    }

    public static class BazelQueryFileType extends LanguageFileType {
        public static final BazelQueryFileType INSTANCE = new BazelQueryFileType();

        private BazelQueryFileType() {
            super(BazelQueryLanguage.INSTANCE);
        }

        @Override
        public String getId() {
            return "BazelQuery";
        }

        @Override
        public LocalizeValue getDescription() {
            return BazelLanguagesLocalize.bazelqueryLanguageDescription();
        }

        @Override
        public String getDefaultExtension() {
            return ".bazelquery";
        }

        @Override
        public Image getIcon() {
            return BazelLanguagesIcons.bazel();
        }
    }
}
