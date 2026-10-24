package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor;

public class ProjectViewPsiImportItem extends ProjectViewBaseElement {
    public ProjectViewPsiImportItem(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(ProjectViewElementVisitor visitor) {
        visitor.visitImportItem(this);
    }
}
