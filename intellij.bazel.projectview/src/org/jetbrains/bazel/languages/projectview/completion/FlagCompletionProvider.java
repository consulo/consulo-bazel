package org.jetbrains.bazel.languages.projectview.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlagCompletionProvider implements CompletionProvider {
    private final String[] myCommands;

    public FlagCompletionProvider(String[] commands) {
        myCommands = commands;
    }

    public String[] getCommands() {
        return myCommands;
    }

    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        ProjectViewPsiSection section = PsiTreeUtil.getParentOfType(parameters.getPosition(), ProjectViewPsiSection.class);
        if (section == null) {
            return;
        }
        List<String> previousFlags = section.getItems().stream().map(it -> it.getText().trim()).filter(it -> !it.isEmpty()).toList();
        PsiElement pos = parameters.getPosition();
        Map<String, Flag> flags = new LinkedHashMap<>();
        for (Map.Entry<String, Flag> it : Flag.all().entrySet()) {
            if (!previousFlags.contains(it.getKey()) &&
                Arrays.stream(myCommands).allMatch(cmd -> Arrays.asList(it.getValue().getOption().commands()).contains(cmd))) {
                flags.put(it.getKey(), it.getValue());
            }
        }
        result.addAllElements(
            flags.entrySet().stream()
                .map(it -> Pair.create(it.getKey(), new BazelFlagSymbol(it.getValue(), pos.getProject())))
                .map(pair -> {
                    String k = pair.getFirst();
                    BazelFlagSymbol flagSymbol = pair.getSecond();
                    return LookupElementBuilder
                        .create(flagSymbol, k)
                        .withTypeText(" " + flagSymbol.getFlag().getOption().valueHelp())
                        .withPresentableText(k)
                        .withIcon(PlatformIconGroup.nodesParameter())
                        .withInsertHandler((ctx, item) -> {
                            PsiElement psiElement = ctx.getFile().findElementAt(ctx.getStartOffset());
                            if (psiElement != null) {
                                if (!psiElement.getText().equals(k)) {
                                    ctx.getEditor()
                                        .getDocument()
                                        .replaceString(psiElement.getTextOffset(), psiElement.getTextRange().getEndOffset(), k);
                                }
                            }
                        });
                })
                .toList()
        );
    }
}
