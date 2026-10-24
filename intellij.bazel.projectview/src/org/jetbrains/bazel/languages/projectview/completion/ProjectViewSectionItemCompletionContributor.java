package org.jetbrains.bazel.languages.projectview.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionType;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.projectview.ProjectViewSection;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName;

import java.util.List;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class ProjectViewSectionItemCompletionContributor extends CompletionContributor implements DumbAware {
    // consulo: upstream top-level public class of this file, kept as a public nested class
    public static class ProjectViewPrefixMatcher extends PrefixMatcher {
        public ProjectViewPrefixMatcher(String prefix) {
            super(prefix);
        }

        @Override
        public boolean prefixMatches(String name) {
            return StringUtil.containsIgnoreCase(name, getPrefix());
        }

        @Override
        public PrefixMatcher cloneWithPrefix(String newPrefix) {
            return new ProjectViewPrefixMatcher(newPrefix);
        }
    }

    // consulo: the nested class AutoPopup is the top-level class AutoPopup below (nested @ExtensionImpl classes are not supported)

    public ProjectViewSectionItemCompletionContributor() {
        ProjectViewSection.getAllRegistered().forEach(section -> {
            CompletionProvider provider = section.getType().getCompletionProvider();
            if (provider == null) {
                return;
            }
            extend(
                CompletionType.BASIC,
                sectionItemElement(section.getKey().getName()),
                provider
            );
        });

        extend(
            CompletionType.BASIC,
            importElement(),
            new PathCompletionProviders.ImportCompletionProvider()
        );
    }

    private PsiElementPattern.Capture<PsiElement> sectionItemElement(String sectionName) {
        return psiElement()
            .withLanguage(ProjectViewLanguage.INSTANCE)
            .withSuperParent(
                2,
                psiElement(ProjectViewPsiSection.class).withFirstChild(
                    psiElement(ProjectViewPsiSectionName.class).withText(sectionName)
                )
            );
    }

    private PsiElementPattern.Capture<PsiElement> importElement() {
        return psiElement()
            .withLanguage(ProjectViewLanguage.INSTANCE)
            .inside(ProjectViewPsiImport.class);
    }

    // consulo: completion contributors are language-bound extensions
    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }
}

// consulo: upstream nested class ProjectViewSectionItemCompletionContributor.AutoPopup (registered as typedHandler);
// Consulo's annotation processor does not support nested @ExtensionImpl classes
@ExtensionImpl
class AutoPopup extends TypedHandlerDelegate {
    private final List<Character> myAcceptedChars = List.of('/', ':', '_');

    public AutoPopup() {
    }

    @Override
    public Result checkAutoPopup(
        char charTyped,
        Project project,
        Editor editor,
        PsiFile file
    ) {
        if (!(file instanceof ProjectViewPsiFile) || (!Character.isLetterOrDigit(charTyped) && !myAcceptedChars.contains(charTyped))) {
            return Result.CONTINUE;
        }

        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return Result.CONTINUE;
    }
}
