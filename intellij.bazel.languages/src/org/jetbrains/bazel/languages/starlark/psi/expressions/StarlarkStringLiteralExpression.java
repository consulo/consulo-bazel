package org.jetbrains.bazel.languages.starlark.psi.expressions;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.LeafElement;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class StarlarkStringLiteralExpression extends StarlarkBaseElement implements PsiLanguageInjectionHost {
    public StarlarkStringLiteralExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitStringLiteralExpression(this);
    }

    public String getStringContents() {
        return getQuote().unwrap(getTextWithQuotes());
    }

    public TextRange getStringContentsOffset() {
        return getQuote().rangeWithinQuotes(getTextWithQuotes()).shiftRight(getRawPrefixLength());
    }

    public StarlarkQuote getQuote() {
        return StarlarkQuote.ofString(getTextWithQuotes());
    }

    public String getTextWithQuotes() {
        return getText().substring(getRawPrefixLength());
    }

    /**
     * Whether this is a raw string literal (`r"..."`), in which backslashes are not escape characters.
     */
    public boolean isRaw() {
        return StringUtil.containsIgnoreCase(getText().substring(0, getRawPrefixLength()), "r");
    }

    private int getRawPrefixLength() {
        String text = getText();
        int end = 0;
        while (end < text.length() && !isQuote(text.charAt(end))) {
            end++;
        }
        String prefix = text.substring(0, end);
        return ALLOWED_STRING_PREFIXES.contains(prefix) ? prefix.length() : 0;
    }

    private static boolean isQuote(@Nullable Character receiver) {
        return receiver != null && (receiver == '\'' || receiver == '"');
    }

    @Override
    public boolean isValidHost() {
        return true;
    }

    @Override
    public PsiLanguageInjectionHost updateText(String text) {
        if (!(getNode().getFirstChildNode() instanceof LeafElement stringNode)) {
            return this;
        }
        stringNode.replaceWithText(text);
        return this;
    }

    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new StarlarkStringLiteralEscaper(this);
    }

    /**
     * Detects whether this string literal is the value of the target name attribute.
     * It ensures that this value represents the whole name - any string concatenation or variable usage is rejected.
     */
    public boolean isTargetNameAttributeValue() {
        if (!(getParent() instanceof StarlarkNamedArgumentExpression parent)) {
            return false;
        }
        if (!parent.isNameArgument()) {
            return false;
        }
        if (!(parent.getParent() instanceof StarlarkArgumentList argList)) {
            return false;
        }
        if (!(argList.getParent() instanceof StarlarkCallExpression)) {
            return false;
        }
        if (!(getContainingFile() instanceof StarlarkFile file)) {
            return false;
        }
        return file.isBuildFile();
    }

    @Override
    public PsiReference[] getReferences() {
        PsiReference[] contributedReferences = ReferenceProvidersRegistry.getReferencesFromProviders(this);
        if (contributedReferences.length != 0) {
            return contributedReferences;
        }
        return super.getReferences();
    }

    @Override
    public @Nullable PsiReference getReference() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? null : support.getReference(this);
    }

    public @Nullable StarlarkArgumentList argumentListContext() {
        if (getParent() instanceof StarlarkArgumentExpression argumentExpression
            && argumentExpression.getParent() instanceof StarlarkArgumentList argumentList) {
            return argumentList;
        }
        return null;
    }

    public boolean isUseExtensionArgument() {
        StarlarkArgumentList argumentList = argumentListContext();
        String calledFunctionName = argumentList != null && argumentList.getParent() instanceof StarlarkCallExpression callExpression
            ? callExpression.getCalledFunctionName()
            : null;
        return Objects.equals(calledFunctionName, "use_extension");
    }

    public @Nullable StarlarkStringLiteralExpression useExtensionLoadedFileName() {
        StarlarkArgumentList argumentList = argumentListContext();
        if (argumentList == null) {
            return null;
        }
        StarlarkArgumentElement[] arguments = argumentList.getArguments();
        if (arguments.length == 0) {
            return null;
        }
        return arguments[0].getFirstChild() instanceof StarlarkStringLiteralExpression literal ? literal : null;
    }

    // capital letters are not allowed here in Starlark
    private static final Set<String> ALLOWED_STRING_PREFIXES = Set.of("", "r", "b", "rb", "br");
}
