package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor;
import org.jetbrains.bazel.languages.projectview.references.ProjectViewLabelReference;
import org.jspecify.annotations.Nullable;

public class ProjectViewPsiSectionItem extends ProjectViewBaseElement {
    public ProjectViewPsiSectionItem(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(ProjectViewElementVisitor visitor) {
        visitor.visitSectionItem(this);
    }

    public @Nullable ProjectViewPsiSection getSection() {
        return getParent() instanceof ProjectViewPsiSection section ? section : null;
    }

    @Override
    public @Nullable PsiReference getReference() {
        ProjectViewPsiSection section = getSection();
        if (section == null) {
            return new ProjectViewLabelReference(this);
        }
        String sectionKeyword = section.getKeyword().getText().trim();
        if (sectionKeyword.equals("targets")) {
            return new ProjectViewLabelReference(this);
        }
        return null;
    }
}
