package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.function.Processor;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementContainer;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public final class SearchUtils {
    private SearchUtils() {
    }

    /**
     * Invokes {@code processor} on each binding in the current scope as well as parent scopes of
     * {@code element} such that a binding is visited before any bindings it may shadow.
     * <p>
     * https://github.com/bazelbuild/starlark/blob/6dd78ee3a66820a8b7571239946466cc702b209e/spec.md#name-binding-and-variables
     */
    @RequiredReadAction
    public static void searchInFile(PsiElement element, Processor<StarlarkElement> processor) {
        PsiElement current = element;
        while (current != null) {
            PsiElement scopeRoot = findScopeRoot(current);
            if (scopeRoot == null) {
                return;
            }
            if (!processBindingsInScope(
                scopeRoot,
                element,
                processor,
                true
            )
            ) {
                return;
            }
            current = scopeRoot.getParent();
        }
    }

    @RequiredReadAction
    private static @Nullable PsiElement findScopeRoot(PsiElement element) {
        if (element instanceof StarlarkFile || element instanceof StarlarkCallable) {
            return element;
        }
        // Default values of parameters refer to the parent scope of the function, not the function scope.
        else if (element instanceof StarlarkParameterList) {
            return findScopeRoot(Objects.requireNonNull(Objects.requireNonNull(element.getParent()).getParent()));
        }
        else {
            PsiElement parent = element.getParent();
            if (parent == null) {
                return null;
            }
            else if (parent instanceof StarlarkForStatement) {
                // For-loop is scope root only when coming from its body (statement list)
                return element instanceof StarlarkStatementList ? parent : findScopeRoot(parent);
            }
            else if (parent instanceof StarlarkCompExpression) {
                // In a comprehension, the first 'for' resolves using the scope that contains the
                // comprehension, while all later 'for's resolve within the comprehension's scope.
                // consulo: Kotlin generateSequence(element) { it.prevSibling }.filter { ... }.take(2).count()
                int count = 0;
                for (PsiElement it = element; it != null && count < 2; it = it.getPrevSibling()) {
                    IElementType elementType = PsiUtilCore.getElementType(it);
                    if (elementType == StarlarkTokenTypes.FOR_KEYWORD || elementType == StarlarkTokenTypes.IF_KEYWORD) {
                        count++;
                    }
                }
                boolean inFirstFor = count == 1;
                if (inFirstFor) {
                    return findScopeRoot(parent);
                }
                else {
                    return parent;
                }
            }
            else {
                return findScopeRoot(parent);
            }
        }
    }

    // consulo: overload for the upstream default argument isScopeRoot = false
    @RequiredReadAction
    private static boolean processBindingsInScope(
        PsiElement scopeRoot,
        PsiElement element,
        Processor<StarlarkElement> processor
    ) {
        return processBindingsInScope(scopeRoot, element, processor, false);
    }

    // return true to continue processing or false to stop.
    @RequiredReadAction
    private static boolean processBindingsInScope(
        PsiElement scopeRoot,
        PsiElement element,
        Processor<StarlarkElement> processor,
        boolean isScopeRoot
    ) {
        if (scopeRoot instanceof StarlarkFile file) {
            if (!file.searchInLoads(processor)) {
                return false;
            }
            for (PsiElement it : file.getChildren()) {
                if (!processBindingsInScope(it, element, processor)) {
                    return false;
                }
            }
            return true;
        }
        else if (scopeRoot instanceof StarlarkCallable callable) {
            if (isScopeRoot) {
                return searchInParameters(callable, processor) &&
                    ifTypeof(callable, StarlarkFunctionDeclaration.class, it -> processBindingsInScope(
                        it.getStatementList(),
                        element,
                        processor
                    ));
            }
            else {
                return ifTypeof(callable, StarlarkFunctionDeclaration.class, it -> processor.process(it));
            }
        }
        else if (scopeRoot instanceof StarlarkStatementContainer container) {
            for (StarlarkStatementList it : container.getStatementLists()) {
                if (!processBindingsInScope(it, element, processor)) {
                    return false;
                }
            }
            // Add loop variables only if this is the scope root (searching from inside
            // this for-loop) or if element is textually after this for-loop
            return ifTypeof(scopeRoot, StarlarkForStatement.class, it -> {
                if (isScopeRoot || element.getTextOffset() > scopeRoot.getTextRange().getEndOffset()) {
                    return searchInLoopVariables(it, processor);
                }
                else {
                    return true;
                }
            });
        }
        else if (scopeRoot instanceof StarlarkCompExpression compExpression) {
            return searchInComprehension(compExpression, processor);
        }
        else if (scopeRoot instanceof StarlarkStatementList statementList) {
            for (PsiElement it : statementList.getChildren()) {
                if (!processBindingsInScope(it, element, processor)) {
                    return false;
                }
            }
            return true;
        }
        else if (scopeRoot instanceof StarlarkAssignmentStatement assignmentStatement) {
            return assignmentStatement.check(processor);
        }
        else {
            return true;
        }
    }

    private static boolean searchInParameters(StarlarkCallable receiver, Processor<StarlarkElement> processor) {
        return receiver.getParameters().stream().allMatch(processor::process);
    }

    @RequiredReadAction
    private static boolean searchInLoopVariables(StarlarkForStatement receiver, Processor<StarlarkElement> processor) {
        return receiver.getLoopVariables().stream().allMatch(processor::process);
    }

    @RequiredReadAction
    private static boolean searchInComprehension(StarlarkCompExpression receiver, Processor<StarlarkElement> processor) {
        return receiver.getCompVariables().stream().allMatch(processor::process);
    }

    private static <T extends StarlarkElement> boolean ifTypeof(PsiElement receiver, Class<T> type, Predicate<T> body) {
        if (type.isInstance(receiver)) {
            return body.test(type.cast(receiver));
        }
        else {
            return true;
        }
    }
}
