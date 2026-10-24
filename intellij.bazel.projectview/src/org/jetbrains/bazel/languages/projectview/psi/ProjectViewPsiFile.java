package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jspecify.annotations.Nullable;

public class ProjectViewPsiFile extends PsiFileBase {
    public ProjectViewPsiFile(FileViewProvider viewProvider) {
        super(viewProvider, ProjectViewLanguage.INSTANCE);
    }

    @Override
    public FileType getFileType() {
        return ProjectViewFileType.INSTANCE;
    }

    public @Nullable ProjectViewPsiSection getSection(String keyword) {
        return ProjectViewPsiSection.findByName(this, keyword);
    }
}
