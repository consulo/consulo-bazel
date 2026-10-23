package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage.BazelrcFileType;

public class BazelrcFile extends PsiFileBase {
    public BazelrcFile(FileViewProvider viewProvider) {
        super(viewProvider, BazelrcLanguage.INSTANCE);
    }

    @Override
    public FileType getFileType() {
        return BazelrcFileType.INSTANCE;
    }

    public BazelrcImport[] getImports() {
        return this.findChildrenByClass(BazelrcImport.class);
    }

    public BazelrcLine[] getLines() {
        return this.findChildrenByClass(BazelrcLine.class);
    }
}
