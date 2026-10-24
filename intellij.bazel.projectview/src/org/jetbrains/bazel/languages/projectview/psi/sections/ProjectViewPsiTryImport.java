package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor;

public class ProjectViewPsiTryImport extends ProjectViewPsiImportBase {
    public ProjectViewPsiTryImport(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(ProjectViewElementVisitor visitor) {
        visitor.visitTryImport(this);
    }

    @Override
    public boolean isImportRequired() {
        return false;
    }
}
