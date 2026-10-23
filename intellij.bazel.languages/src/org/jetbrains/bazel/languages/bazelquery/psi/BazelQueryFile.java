package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage.BazelQueryFlagsFileType;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage.BazelQueryFileType;

public class BazelQueryFile extends PsiFileBase {
    public BazelQueryFile(FileViewProvider viewProvider) {
        super(viewProvider, BazelQueryLanguage.INSTANCE);
    }

    @Override
    public FileType getFileType() {
        return BazelQueryFileType.INSTANCE;
    }

    public static class BazelQueryFlagsFile extends PsiFileBase {
        public BazelQueryFlagsFile(FileViewProvider viewProvider) {
            super(viewProvider, BazelQueryFlagsLanguage.INSTANCE);
        }

        @Override
        public FileType getFileType() {
            return BazelQueryFlagsFileType.INSTANCE;
        }
    }
}
