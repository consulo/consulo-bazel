package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@ExtensionImpl
public class StarlarkReferenceSupportImpl implements StarlarkReferenceSupport {
    @Override
    @RequiredReadAction
    public @Nullable PsiReference getReference(StarlarkReferenceExpression element) {
        if (isThrowaway(element)) {
            return null;
        }
        else if (element.isQualified()) {
            return new StarlarkQualifiedReferenceExpressionReference(element);
        }
        else if (element.getParent() instanceof StarlarkCallExpression callExpression
            && Objects.equals(callExpression.getCalledExpression(), element)
            && !isBeforeDot(element)) {
            PsiElement identifier = element.getNameIdentifier();
            if (identifier == null) {
                return null;
            }
            else {
                return new StarlarkFunctionCallReference(element, identifier.getTextRangeInParent());
            }
        }
        else {
            return new StarlarkLocalVariableReference(element, false);
        }
    }

    @Override
    @RequiredReadAction
    public PsiReference getReference(StarlarkTargetExpression element) {
        return new StarlarkLocalVariableReference(element, true);
    }

    @Override
    @RequiredReadAction
    public PsiReference getReference(StarlarkArgumentExpression element) {
        return new StarlarkArgumentReference(element, element.getTextRange());
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiReference getReference(StarlarkNamedArgumentExpression element) {
        ASTNode nameNode = element.getNameNode();
        if (nameNode == null) {
            return null;
        }
        TextRange range = StarlarkBaseElement.relativeTo(nameNode.getTextRange(), element);
        return new StarlarkNamedArgumentReference(element, range);
    }

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    @Override
    @RequiredReadAction
    public Collection<PsiReference> getOwnReferences(StarlarkNamedArgumentExpression element) {
        return element.getName() != null ? List.of(new BazelGlobalFunctionArgumentReference(element)) : List.of();
    }

    @Override
    @RequiredReadAction
    public PsiReference getReference(StarlarkGlobExpression element) {
        return new StarlarkGlobReference(element);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiReference getReference(StarlarkStringLiteralExpression element) {
        if (isInVisibilityList(element)) {
            return new StarlarkVisibilityReference(element);
        }
        if (isLoadFilenameValue(element)) {
            return new BazelLabelReference(element, true);
        }
        if (element.isUseExtensionArgument()) {
            StarlarkStringLiteralExpression loadedFileNamePsi = element.useExtensionLoadedFileName();
            if (loadedFileNamePsi == null) {
                return null;
            }
            BazelLabelReference loadedFileReference = new BazelLabelReference(loadedFileNamePsi, true);
            PsiReference reference;
            if (loadedFileNamePsi.equals(element)) {
                reference = loadedFileReference;
            }
            else {
                reference = new StarlarkLoadReference(element, loadedFileReference);
            }
            return reference;
        }
        StarlarkLoadStatement loadAncestor = findLoadStatement(element);
        if (loadAncestor == null) {
            return new BazelLabelReference(element, true);
        }
        StarlarkStringLiteralExpression loadedFileNamePsi = loadAncestor.getLoadedFileNamePsi();
        if (loadedFileNamePsi == null) {
            return null;
        }
        BazelLabelReference loadedFileReference = new BazelLabelReference(loadedFileNamePsi, true);
        if (loadedFileNamePsi.equals(element)) {
            return loadedFileReference;
        }
        else {
            return new StarlarkLoadReference(element, loadedFileReference);
        }
    }

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    @Override
    @RequiredReadAction
    public Collection<PsiReference> getOwnReferences(StarlarkCallExpression element) {
        String name = element.getCalledFunctionName();
        if (name == null) {
            return List.of();
        }
        BazelGlobalFunction function = BazelGlobalFunctions.getFunctionByName(name, element.getProject());
        if (function == null) {
            return List.of();
        }
        List<PsiReference> references = new ArrayList<>();
        references.add(new BazelGlobalFunctionReference(element, function));
        // consulo: upstream also adds `getCalledExpression()?.reference` wrapped by PsiSymbolService.asSymbolReference;
        // with classic references the called expression keeps its own reference (found by findReferenceAt), so it is not repeated here
        return references;
    }

    @RequiredReadAction
    private static boolean isBeforeDot(StarlarkReferenceExpression receiver) {
        for (ASTNode it = receiver.getNode().getTreeNext(); it != null; it = it.getTreeNext()) {
            if (it.getElementType() == StarlarkTokenTypes.DOT) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    private static boolean isThrowaway(StarlarkReferenceExpression receiver) {
        return Objects.equals(receiver.getName(), "_");
    }

    @RequiredReadAction
    private static boolean isLoadFilenameValue(StarlarkStringLiteralExpression receiver) {
        return receiver.getParent() instanceof StarlarkFilenameLoadValue;
    }

    @RequiredReadAction
    private static @Nullable StarlarkLoadStatement findLoadStatement(StarlarkStringLiteralExpression receiver) {
        return receiver.getParent() instanceof StarlarkLoadValue loadValue ? loadValue.getLoadStatement() : null;
    }

    @RequiredReadAction
    private static boolean isInVisibilityList(StarlarkStringLiteralExpression receiver) {
        PsiElement parent = receiver.getParent();
        if (!(parent instanceof StarlarkListLiteralExpression)) {
            return false;
        }
        String name = parent.getParent() instanceof StarlarkNamedArgumentExpression namedArgument ? namedArgument.getName() : null;
        // consulo: List.of(...).contains(null) throws, Kotlin `in` returns false
        return name != null && List.of(
            "visibility",
            "default_visibility"
        ).contains(name);
    }
}
