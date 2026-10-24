package org.jetbrains.bazel.languages.projectview.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;

import java.util.List;

public class SimpleCompletionProvider implements CompletionProvider {
    private final List<String> myVariants;

    public SimpleCompletionProvider(List<String> variants) {
        myVariants = variants;
    }

    public List<String> getVariants() {
        return myVariants;
    }

    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        result.addAllElements(myVariants.stream().map(this::sectionItemLookupElement).toList());
    }

    private LookupElement sectionItemLookupElement(String value) {
        return LookupElementBuilder
            .create(value)
            .withIcon(PlatformIconGroup.nodesProperty());
    }
}
