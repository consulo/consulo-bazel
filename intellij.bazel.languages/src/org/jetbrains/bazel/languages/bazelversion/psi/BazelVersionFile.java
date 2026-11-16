package org.jetbrains.bazel.languages.bazelversion.psi;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage.BazelVersionFileType;
import org.jspecify.annotations.Nullable;

public class BazelVersionFile extends PsiFileBase {
    public BazelVersionFile(FileViewProvider viewProvider) {
        super(viewProvider, BazelVersionLanguage.INSTANCE);
    }

    @Override
    public FileType getFileType() {
        return BazelVersionFileType.INSTANCE;
    }

    public @Nullable BazelVersionLiteral getBazelVersion() {
        return BazelVersionLiteral.fromString(this.getText());
    }
}
