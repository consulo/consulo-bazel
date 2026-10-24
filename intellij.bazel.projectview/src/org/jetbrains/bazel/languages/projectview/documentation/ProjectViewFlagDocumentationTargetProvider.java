package org.jetbrains.bazel.languages.projectview.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.bazel.documentation.DocumentationTargetProvider;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.BazelFlagSymbolLessDocumentationTarget;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;

import java.util.List;

@ExtensionImpl
class ProjectViewFlagDocumentationTargetProvider implements DocumentationTargetProvider {
    public ProjectViewFlagDocumentationTargetProvider() {
    }

    @Override
    public List<? extends DocumentationTarget> documentationTargets(PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return List.of();
        }
        if (!(element.getLanguage() instanceof ProjectViewLanguage)) {
            return List.of();
        }
        if (!(element instanceof LeafPsiElement)) {
            return List.of();
        }
        if (!(element.getParent() instanceof ProjectViewPsiSectionItem)) {
            return List.of();
        }
        Flag flag = Flag.byName(element.getText());
        if (flag == null) {
            return List.of();
        }
        return List.of(new BazelFlagSymbolLessDocumentationTarget(element, null, flag));
    }
}
