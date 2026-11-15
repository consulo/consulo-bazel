package org.jetbrains.bazel.languages.starlark.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class StarlarkParamsCompletionContributor extends CompletionContributor {
    public StarlarkParamsCompletionContributor() {
        extend(CompletionType.BASIC, paramElement("*"), new StarlarkParamsCompletionProvider("args"));
        extend(CompletionType.BASIC, paramElement("**"), new StarlarkParamsCompletionProvider("kwargs"));
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    private static PsiElementPattern.Capture<PsiElement> paramElement(String token) {
        return psiElement().inside(StarlarkParameterList.class).afterLeaf(token);
    }
}

class StarlarkParamsCompletionProvider implements CompletionProvider {
    private final String myLookupString;

    StarlarkParamsCompletionProvider(String lookupString) {
        myLookupString = lookupString;
    }

    public String getLookupString() {
        return myLookupString;
    }

    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        result.addElement(LookupElementBuilder.create(myLookupString));
    }
}
