package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

import static consulo.language.psi.util.PsiTreeUtil.getChildOfType;

public class StarlarkQualifiedReferenceExpressionReference extends PsiReferenceBase<StarlarkReferenceExpression> {
    @RequiredReadAction
    public StarlarkQualifiedReferenceExpressionReference(StarlarkReferenceExpression element) {
        super(
            element,
            calculateRange(element),
            false
        );
    }

    // consulo: upstream `element.getNameIdentifier()?.textRangeInParent ?: TextRange(0, element.textLength)` in the super call
    @RequiredReadAction
    private static TextRange calculateRange(StarlarkReferenceExpression element) {
        PsiElement nameIdentifier = element.getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getTextRangeInParent() : new TextRange(0, element.getTextLength());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String identifier = myElement.getName();
        if (identifier == null) {
            return null;
        }
        StarlarkArgumentList structArgumentList = getStructArgumentList(myElement);
        StarlarkNamedArgumentExpression definition = structArgumentList == null ? null : structArgumentList.getKeywordArgument(identifier);
        if (definition == null) {
            return null;
        }

        PsiReference reference = definition.getLastChild() instanceof StarlarkReferenceExpression referenceExpression
            ? referenceExpression.getReference()
            : null;
        PsiElement resolved = reference == null ? null : reference.resolve();
        return resolved != null ? resolved : definition;
    }

    @Override
    @RequiredReadAction
    public LookupElementBuilder[] getVariants() {
        StarlarkArgumentList structArgumentList = getStructArgumentList(myElement);
        if (structArgumentList == null) {
            return new LookupElementBuilder[0];
        }
        return Arrays.stream(structArgumentList.getArguments())
            .map(it -> it.getName())
            .filter(Objects::nonNull)
            .map(it -> LookupElementBuilder.create(it))
            .toArray(LookupElementBuilder[]::new);
    }

    @RequiredReadAction
    private static @Nullable StarlarkArgumentList getStructArgumentList(StarlarkReferenceExpression receiver) {
        PsiElement qualifierExpression = receiver.getQualifierExpression();
        PsiReference qualifierReference = qualifierExpression == null ? null : qualifierExpression.getReference();
        PsiElement qualifiedPartResolved = qualifierReference == null ? null : qualifierReference.resolve();
        if (qualifiedPartResolved == null) {
            return null;
        }
        StarlarkCallExpression structCall;
        if (qualifiedPartResolved instanceof StarlarkNamedArgumentExpression namedArgumentExpression) {
            structCall = namedArgumentExpression.getValue() instanceof StarlarkCallExpression callExpression ? callExpression : null;
        }
        else {
            structCall = qualifiedPartResolved.getParent() instanceof StarlarkAssignmentStatement assignmentStatement
                ? getChildOfType(assignmentStatement, StarlarkCallExpression.class)
                : null;
        }
        if (structCall == null) {
            return null;
        }
        if (!Objects.equals(structCall.getCalledFunctionName(), "struct")) {
            return null;
        }
        return structCall.getArgumentList();
    }
}
