package org.jetbrains.bazel.languages.projectview.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import org.jetbrains.bazel.languages.projectview.ProjectViewSection;
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionType;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;

import static consulo.language.pattern.PlatformPatterns.psiComment;
import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
class ProjectViewSectionCompletionContributor extends CompletionContributor implements DumbAware {
    public ProjectViewSectionCompletionContributor() {
        extend(
            CompletionType.BASIC,
            projectViewSectionElement(),
            new ProjectViewSectionCompletionProvider()
        );
    }

    private PsiElementPattern.Capture<PsiElement> projectViewSectionElement() {
        return psiElement()
            .withLanguage(ProjectViewLanguage.INSTANCE)
            .withSuperParent(2, ProjectViewPsiFile.class)
            .inFile(psiElement(ProjectViewPsiFile.class))
            .andNot(psiElement().afterLeaf(psiElement(ProjectViewTokenType.COLON)))
            .andNot(psiComment());
    }

    // consulo: completion contributors are language-bound extensions
    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }

    private static class ProjectViewSectionCompletionProvider implements CompletionProvider {
        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            result.addElement(LookupElementBuilder.create("import").withIcon(PlatformIconGroup.nodesFunction()));
            result.addAllElements(
                ProjectViewSection.getAllRegistered()
                    .filter(it -> {
                        ProjectViewSection.Deprecation deprecation = it.getDeprecation();
                        return deprecation == null || deprecation.getLevel() == ProjectViewSection.Deprecation.Level.Warning;
                    })
                    .map(this::lookupElement)
                    .toList()
            );
        }

        private static class SectionInsertionHandle<T extends LookupElement> implements InsertHandler<T> {
            private final boolean myIsScalar;

            SectionInsertionHandle(boolean isScalar) {
                myIsScalar = isScalar;
            }

            @Override
            public void handleInsert(InsertionContext context, T item) {
                Editor editor = context.getEditor();
                Document document = editor.getDocument();
                document.insertString(context.getTailOffset(), ": ");
                if (myIsScalar) {
                    editor.getCaretModel().moveToOffset(context.getTailOffset());
                }
                else {
                    document.insertString(context.getTailOffset(), "\n  ");
                    editor.getCaretModel().moveToOffset(context.getTailOffset());
                }
            }
        }

        private LookupElement lookupElement(ProjectViewSection<?> receiver) {
            LookupElementBuilder it = LookupElementBuilder
                .create(receiver.getKey().getName())
                .withIcon(PlatformIconGroup.nodesFunction())
                .withInsertHandler(new SectionInsertionHandle<>(ProjectViewSectionType.isScalar(receiver.getType())))
                .withStrikeoutness(receiver.getDeprecation() != null);
            if (receiver.getDeprecation() == null) {
                return it;
            }
            else {
                return PrioritizedLookupElement.withPriority(it, -1.0);
            }
        }
    }
}
