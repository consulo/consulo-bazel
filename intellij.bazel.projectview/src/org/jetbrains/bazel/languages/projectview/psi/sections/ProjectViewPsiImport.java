package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor;

public class ProjectViewPsiImport extends ProjectViewPsiImportBase {
    public ProjectViewPsiImport(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(ProjectViewElementVisitor visitor) {
        visitor.visitImport(this);
    }

    @Override
    public boolean isImportRequired() {
        return true;
    }
}
