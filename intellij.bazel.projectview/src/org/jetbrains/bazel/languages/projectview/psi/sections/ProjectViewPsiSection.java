package org.jetbrains.bazel.languages.projectview.psi.sections;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ProjectViewPsiSection extends ProjectViewBaseElement {
    public ProjectViewPsiSection(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(ProjectViewElementVisitor visitor) {
        visitor.visitSection(this);
    }

    public PsiElement getKeyword() {
        return Objects.requireNonNull(getFirstChild());
    }

    public List<ProjectViewPsiSectionItem> getItems() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, ProjectViewPsiSectionItem.class);
    }

    public @Nullable PsiElement getColon() {
        return findChildByType(ProjectViewTokenType.COLON);
    }

    public static @Nullable ProjectViewPsiSection findByName(PsiElement psi, String name) {
        return PsiTreeUtil.getChildrenOfTypeAsList(psi, ProjectViewPsiSection.class)
            .stream()
            .filter(it -> sectionName(it).equals(name))
            .findFirst()
            .orElse(null);
    }

    // consulo: upstream internal top-level extension function ProjectViewPsiSection.sectionName()
    public static String sectionName(ProjectViewPsiSection receiver) {
        return receiver.getKeyword().getText().trim();
    }
}
