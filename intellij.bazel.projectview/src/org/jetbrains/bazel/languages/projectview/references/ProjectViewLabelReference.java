package org.jetbrains.bazel.languages.projectview.references;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewCodeInsightSupport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jspecify.annotations.Nullable;

import static org.jetbrains.bazel.config.project.isBazelProject;

// consulo: the upstream property `element` is PsiReferenceBase.getElement()
public class ProjectViewLabelReference extends PsiReferenceBase<ProjectViewPsiSectionItem> {
    public ProjectViewLabelReference(ProjectViewPsiSectionItem element) {
        super(element, new TextRange(0, element.getTextLength()));
    }

    @Override
    public @Nullable PsiElement resolve() {
        ProjectViewPsiSectionItem element = getElement();
        Project project = element.getProject();
        if (!isBazelProject(project)) {
            return null;
        }
        String text = element.getText();
        Label label = Label.parseOrNull(text);
        if (label == null) {
            return null;
        }
        return project.getInstance(ProjectViewCodeInsightSupport.class)
            .resolvePsiFromLabel(label, null, false);
    }
}
