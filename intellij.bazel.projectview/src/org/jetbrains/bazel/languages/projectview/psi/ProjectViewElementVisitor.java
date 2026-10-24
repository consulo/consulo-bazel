package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.psi.PsiElementVisitor;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiTryImport;

public class ProjectViewElementVisitor extends PsiElementVisitor {
    public void visitImport(ProjectViewPsiImport node) {
        visitElement(node);
    }

    public void visitTryImport(ProjectViewPsiTryImport node) {
        visitElement(node);
    }

    public void visitImportItem(ProjectViewPsiImportItem node) {
        visitElement(node);
    }

    public void visitSection(ProjectViewPsiSection node) {
        visitElement(node);
    }

    public void visitSectionItem(ProjectViewPsiSectionItem node) {
        visitElement(node);
    }

    public void visitSectionName(ProjectViewPsiSectionName node) {
        visitElement(node);
    }
}
