package org.jetbrains.bazel.languages.bazelquery;

import consulo.bazel.languages.localize.BazelLanguagesLocalize;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;

public class BazelQueryFlagsLanguage extends Language {
    public static final BazelQueryFlagsLanguage INSTANCE = new BazelQueryFlagsLanguage();

    private BazelQueryFlagsLanguage() {
        super("BazelQueryFlags");
    }

    public static class BazelQueryFlagsFileType extends LanguageFileType {
        public static final BazelQueryFlagsFileType INSTANCE = new BazelQueryFlagsFileType();

        private BazelQueryFlagsFileType() {
            super(BazelQueryFlagsLanguage.INSTANCE);
        }

        @Override
        public String getId() {
            return "BazelQueryFlags";
        }

        @Override
        public LocalizeValue getDescription() {
            return BazelLanguagesLocalize.bazelqueryFlagsLanguageDescription();
        }

        @Override
        public String getDefaultExtension() {
            return ".bazelqueryflags";
        }

        @Override
        public Image getIcon() {
            return BazelLanguagesIcons.bazel();
        }
    }
}
