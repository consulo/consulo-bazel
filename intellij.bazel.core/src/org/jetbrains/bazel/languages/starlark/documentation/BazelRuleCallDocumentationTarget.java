package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.application.util.HtmlChunk;
import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;

public class BazelRuleCallDocumentationTarget implements DocumentationTarget {
    private final StarlarkCallExpression myTargetExpression;

    public BazelRuleCallDocumentationTarget(StarlarkCallExpression targetExpression) {
        myTargetExpression = targetExpression;
    }

    public StarlarkCallExpression getTargetExpression() {
        return myTargetExpression;
    }

    // consulo: createPointer() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public String computeDocumentationHint() {
        return HtmlChunk
            .tag("pre")
            .style("white-space: pre; overflow-x: auto")
            .child(asHighlightedHtmlWithLabelLinks(myTargetExpression))
            .toString();
    }

    @Override
    public DocumentationResult computeDocumentation() {
        HtmlChunk.Element pre = HtmlChunk.tag("pre")
            .style("white-space: pre; overflow-x: auto")
            .child(asHighlightedHtmlWithLabelLinks(myTargetExpression));
        // consulo: Consulo DocumentationMarkup has no CLASS_DEFINITION, its IntelliJ value is "definition"
        HtmlChunk.Element definition = HtmlChunk.div().setClass("definition").child(pre);
        return DocumentationResult.documentation(definition.toString());
    }

    private static HtmlChunk asHighlightedHtmlWithLabelLinks(StarlarkElement receiver) {
        return StarlarkPsiToHtml.asHighlightedHtml(receiver, it -> {
            if (!(it.getParent() instanceof StarlarkStringLiteralExpression literal)) {
                return null;
            }
            return StarlarkDocumentationLinks.forTargetLabel(literal);
        });
    }
}
