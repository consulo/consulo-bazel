package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionProcessor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class StarlarkLoadReference extends PsiReferenceBase<StarlarkStringLiteralExpression> {
    private final BazelLabelReference myLoadedFileReference;

    @RequiredReadAction
    public StarlarkLoadReference(StarlarkStringLiteralExpression element, BazelLabelReference loadedFileReference) {
        super(element, new TextRange(0, element.getTextLength()), false);
        myLoadedFileReference = loadedFileReference;
    }

    public BazelLabelReference getLoadedFileReference() {
        return myLoadedFileReference;
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        if (!(myLoadedFileReference.resolve() instanceof StarlarkFile loadedFile)) {
            return null;
        }
        String name = myElement.getStringContents();
        StarlarkResolveNameProcessor processor = new StarlarkResolveNameProcessor(new ArrayList<>(), name);
        SearchUtils.searchInFile(loadedFile, processor);
        return processor.getResult().isEmpty() ? null : processor.getResult().get(0);
    }

    @Override
    @RequiredReadAction
    public LookupElement[] getVariants() {
        StarlarkStringLiteralExpression element = myElement;
        if (!(myLoadedFileReference.resolve() instanceof StarlarkFile loadedFile)) {
            return LookupElement.EMPTY_ARRAY;
        }
        StarlarkCompletionProcessor processor = new StarlarkCompletionProcessor(new HashMap<>(), element, element.getQuote());
        SearchUtils.searchInFile(loadedFile, processor);
        return processor.getResults().values().toArray(LookupElement.EMPTY_ARRAY);
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String newElementName) {
        ASTNode newNode = new StarlarkElementGenerator(myElement.getProject()).createStringLiteral(newElementName);
        myElement.getNode().replaceChild(Objects.requireNonNull(myElement.getNode().getFirstChildNode()), newNode);
        return myElement;
    }
}
