package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionProcessor;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class StarlarkLocalVariableReference extends PsiReferenceBase<StarlarkLocalVariableElement> {
    @RequiredReadAction
    public StarlarkLocalVariableReference(
        StarlarkLocalVariableElement element,
        boolean soft
    ) {
        super(element, new TextRange(0, element.getTextLength()), soft);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        StarlarkLocalVariableElement element = myElement;
        StarlarkResolveProcessor processor = new StarlarkResolveProcessor(new ArrayList<>(), element, element.getName());
        SearchUtils.searchInFile(element, processor);
        StarlarkElement resolved = processor.getResult().isEmpty() ? null : processor.getResult().get(0);
        if (!(element instanceof StarlarkTargetExpression)) {
            return resolved;
        }
        if (resolved == null) {
            return null;
        }
        // take if it's reassignment in the same scope
        return resolved.getTextOffset() <= element.getTextOffset() && Objects.equals(resolved.getUseScope(), element.getUseScope())
            ? resolved
            : null;
    }

    @Override
    @RequiredReadAction
    public StarlarkLookupElement[] getVariants() {
        StarlarkCompletionProcessor processor = new StarlarkCompletionProcessor(new HashMap<>(), myElement);
        SearchUtils.searchInFile(myElement, processor);
        return processor.getResults().values().toArray(new StarlarkLookupElement[0]);
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String name) {
        PsiElement nameIdentifier = myElement.getNameIdentifier();
        ASTNode oldNode = nameIdentifier == null ? null : nameIdentifier.getNode();
        if (oldNode == null) {
            return myElement;
        }
        ASTNode newNode = RenameUtils.createNewName(myElement.getProject(), name);
        myElement.getNode().replaceChild(oldNode, newNode);
        return myElement;
    }
}
