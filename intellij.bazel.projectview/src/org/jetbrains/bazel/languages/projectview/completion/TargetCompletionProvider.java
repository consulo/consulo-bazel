package org.jetbrains.bazel.languages.projectview.completion;

import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.projectview.ProjectViewCodeInsightSupport;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType;
import org.jetbrains.bazel.languages.projectview.completion.ProjectViewSectionItemCompletionContributor.ProjectViewPrefixMatcher;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;

public class TargetCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        Project project = parameters.getPosition().getProject();
        String positionText = parameters.getPosition().getText();
        // consulo: Kotlin take(n).removePrefix("-")
        String prefix =
            StringUtil.trimStart(
                positionText.substring(
                    0,
                    Math.min(parameters.getOffset() - parameters.getPosition().getTextRange().getStartOffset(), positionText.length())
                ),
                "-"
            );

        PsiElement prevSibling = parameters.getPosition().getPrevSibling();
        ASTNode prevSiblingNode = prevSibling != null ? prevSibling.getNode() : null;
        if (prevSiblingNode != null && prevSiblingNode.getElementType() == ProjectViewTokenType.COLON) {
            prefix = ":" + prefix;
        }

        CompletionResultSet resultWithPrefixMatcher = result.withPrefixMatcher(new ProjectViewPrefixMatcher(prefix));
        resultWithPrefixMatcher.addAllElements(
            project.getInstance(ProjectViewCodeInsightSupport.class).getAllTargetLabels().map(this::labelLookupElement)
                .toList()
        );
    }

    private LookupElement labelLookupElement(String label) {
        return LookupElementBuilder
            .create(label)
            .withIcon(ProjectViewFileType.INSTANCE.getBazelIcon());
    }
}
