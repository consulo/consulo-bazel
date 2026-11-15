package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SyntaxTraverser;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkSemanticHighlighting;
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkSyntaxHighlighter;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public final class StarlarkPsiToHtml {
    private StarlarkPsiToHtml() {
    }

    // consulo: overload for the upstream default argument (linkBuilder = { null })
    public static HtmlChunk asHighlightedHtml(StarlarkElement receiver) {
        return asHighlightedHtml(receiver, it -> null);
    }

    public static HtmlChunk asHighlightedHtml(StarlarkElement receiver, Function<PsiElement, @Nullable String> linkBuilder) {
        HtmlBuilder builder = new HtmlBuilder();
        for (PsiElement leaf : SyntaxTraverser.psiTraverser(receiver).preOrderDfsTraversal()) {
            if (leaf.getFirstChild() != null) {
                continue;
            }
            HtmlChunk chunk = starlarkHighlightedHtmlChunk(leaf);
            String link = linkBuilder.apply(leaf);
            builder.append(link != null ? HtmlChunk.link(link, chunk) : chunk);
        }
        return builder.toFragment();
    }

    private static HtmlChunk starlarkHighlightedHtmlChunk(PsiElement receiver) {
        TextAttributesKey key = starlarkHighlightingColor(receiver);
        if (key == null) {
            return HtmlChunk.text(receiver.getText());
        }
        // consulo: QuickDocHighlightingHelper.getStyledFragment(text, key) does not exist in Consulo,
        // the escaped text is not styled
        return HtmlChunk.raw(XmlStringUtil.escapeString(receiver.getText()));
    }

    private static @Nullable TextAttributesKey starlarkHighlightingColor(PsiElement receiver) {
        TextAttributesKey semantic = StarlarkSemanticHighlighting.starlarkSemanticHighlightingColor(receiver);
        if (semantic != null) {
            return semantic;
        }
        IElementType elementType = PsiUtilCore.getElementType(receiver);
        if (elementType == null) {
            return null;
        }
        TextAttributesKey[] highlights = StarlarkSyntaxHighlighter.INSTANCE.getTokenHighlights(elementType);
        return highlights.length == 0 ? null : highlights[0];
    }
}

final class StarlarkDocumentationLinks {
    private StarlarkDocumentationLinks() {
    }

    static @Nullable String forTargetLabel(StarlarkStringLiteralExpression element) {
        PsiReference reference = element.getReference();
        PsiElement referenced = reference == null ? null : reference.resolve();
        if (referenced == null) {
            return null;
        }
        if (!StarlarkCallExpression.isRuleTarget(referenced)) {
            return null;
        }
        String labelText = element.getStringContents();
        return DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL + labelText;
    }

    static @Nullable Label labelFrom(String link) {
        if (!link.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
            return null;
        }
        String labelText = StringUtil.trimStart(link, DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL);
        return Label.parseOrNull(labelText);
    }
}
