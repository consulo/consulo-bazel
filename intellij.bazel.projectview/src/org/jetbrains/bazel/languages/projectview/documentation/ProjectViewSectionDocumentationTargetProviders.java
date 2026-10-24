package org.jetbrains.bazel.languages.projectview.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.bazel.documentation.DocumentationTargetProvider;
import consulo.bazel.documentation.LookupElementDocumentationTargetProvider;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.xml.XmlStringUtil;
import org.jetbrains.bazel.languages.projectview.ProjectViewSection;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static consulo.language.pattern.PlatformPatterns.psiElement;

// consulo: this file declares only package-private top-level classes (registered with @ExtensionImpl, which does not
// support nested classes), so no class is named after the file

@ExtensionImpl
class ProjectViewSectionDocumentationTargetProvider implements DocumentationTargetProvider {
    public ProjectViewSectionDocumentationTargetProvider() {
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
        if (!(element.getParent() instanceof ProjectViewPsiSectionName)) {
            return List.of();
        }
        ProjectViewSection<?> section = ProjectViewSection.findByPsi(element);
        if (section == null) {
            return List.of();
        }
        return List.of(new SectionDocumentationTarget(section));
    }
}

@ExtensionImpl
class ProjectViewSectionLookupElementDocumentationTargetProvider implements LookupElementDocumentationTargetProvider {
    public ProjectViewSectionLookupElementDocumentationTargetProvider() {
    }

    @Override
    public @Nullable DocumentationTarget documentationTarget(
        PsiFile psiFile,
        LookupElement lookupElement,
        int offset
    ) {
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (psiElement == null) {
            return null;
        }
        if (!PROJECT_VIEW_SECTION_ELEMENT.accepts(psiElement)) {
            return null;
        }
        ProjectViewSection<?> section = ProjectViewSection.findByName(lookupElement.getLookupString());
        if (section == null) {
            return null;
        }
        return new SectionDocumentationTarget(section);
    }

    // companion object

    private static final PsiElementPattern.Capture<PsiElement> PROJECT_VIEW_SECTION_ELEMENT =
        psiElement()
            .withLanguage(ProjectViewLanguage.INSTANCE)
            .withSuperParent(2, ProjectViewPsiFile.class)
            .inFile(psiElement(ProjectViewPsiFile.class));
}

class SectionDocumentationTarget implements DocumentationTarget {
    private final ProjectViewSection<?> mySection;

    SectionDocumentationTarget(
        ProjectViewSection<?> section
    ) {
        mySection = section;
    }

    // consulo: createPointer(), dereference() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public DocumentationResult computeDocumentation() {
        // consulo: QuickDocHighlightingHelper.getStyledFragment(name, ProjectViewHighlightingColors.KEYWORD) does not exist in Consulo,
        // the section name is bold escaped text
        String sectionName = "<b>" + XmlStringUtil.escapeText(mySection.getKey().getName()) + "</b>";
        String documentation = "<pre>" + sectionName + "</pre><hr/> " + mySection.getDocumentation().get();
        return DocumentationResult.documentation(documentation);
    }
}
