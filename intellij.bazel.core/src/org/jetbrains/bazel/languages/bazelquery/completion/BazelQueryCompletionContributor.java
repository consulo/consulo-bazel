package org.jetbrains.bazel.languages.bazelquery.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenType;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFile;

import java.util.Arrays;
import java.util.List;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
class BazelQueryCompletionContributor extends CompletionContributor {
    public BazelQueryCompletionContributor() {
        extend(CompletionType.BASIC, BazelWordCompletionProvider.psiPattern, new BazelWordCompletionProvider());
        extend(CompletionType.BASIC, BazelInfixCompletionProvider.psiPattern, new BazelInfixCompletionProvider());
    }

    // consulo: completion contributors are language-bound extensions (upstream XML language="BazelQuery")
    @Override
    public Language getLanguage() {
        return BazelQueryLanguage.INSTANCE;
    }

    // consulo: upstream top-level declarations of BazelQueryCompletionContributor.kt are static members of this class

    public static final List<String> knownCommands =
        Arrays.stream(BazelQueryTokenSets.COMMANDS.getTypes())
            .map(tokenType -> {
                BazelQueryTokenType type = (BazelQueryTokenType) tokenType;
                return type.completionText() + "()";
            })
            .toList();

    public static final List<String> knownOperations =
        List.of(
            BazelQueryTokenTypes.LET.completionText() + "  in",
            BazelQueryTokenTypes.SET.completionText() + "()"
        );

    static final List<String> knownInfixOperators =
        Arrays.stream(BazelQueryTokenSets.INFIX_OPERATORS.getTypes())
            .map(tokenType -> {
                BazelQueryTokenType type = (BazelQueryTokenType) tokenType;
                return type.completionText();
            })
            .toList();

    private static final TokenSet WORDS_OR_COMMANDS_TOKENS =
        TokenSet.create(
            concat(
                BazelQueryTokenSets.WORDS.getTypes(),
                BazelQueryTokenSets.COMMANDS.getTypes(),
                BazelQueryTokenSets.PATTERNS.getTypes()
            )
        );

    // consulo: Kotlin spread operator `*a, *b, *c`
    private static IElementType[] concat(IElementType[]... arrays) {
        return Arrays.stream(arrays).flatMap(Arrays::stream).toArray(IElementType[]::new);
    }

    private static class BazelInfixCompletionProvider implements CompletionProvider {
        static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement()
                .withLanguage(BazelQueryLanguage.INSTANCE)
                .and(
                    psiElement().inside(psiElement(BazelQueryFile.class))
                ).andOr(
                    psiElement().afterLeaf(psiElement(BazelQueryTokenTypes.RIGHT_PAREN)),
                    psiElement().afterLeaf(psiElement().withElementType(BazelQueryTokenSets.WORDS))
                );

        @Override
        @RequiredReadAction
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            // consulo: Kotlin text.take(n).trim().removePrefix("\"").removePrefix("'")
            String text = parameters.getPosition().getText();
            int length = parameters.getOffset() - parameters.getPosition().getTextRange().getStartOffset();
            String prefix =
                StringUtil.trimStart(
                    StringUtil.trimStart(
                        text.substring(0, Math.min(length, text.length()))
                            .trim(),
                        "\""
                    ),
                    "'"
                );

            CompletionResultSet it = result.withPrefixMatcher(new BazelQueryPrefixMatcher(prefix));
            it.addAllElements(
                knownInfixOperators.stream().map(op -> functionLookupElement(op, 0.0, parameters.getOffset())).toList()
            );
        }
    }

    private static class BazelWordCompletionProvider implements CompletionProvider {
        private final String myStartTargetSign = "//";

        static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement()
                .withLanguage(BazelQueryLanguage.INSTANCE)
                .and(
                    psiElement().inside(psiElement(BazelQueryFile.class))
                ).andOr(
                    psiElement().withElementType(WORDS_OR_COMMANDS_TOKENS),
                    psiElement().atStartOf(psiElement().withElementType(WORDS_OR_COMMANDS_TOKENS))
                ).andNot(
                    psiElement().afterLeaf(psiElement(BazelQueryTokenTypes.RIGHT_PAREN))
                ).andNot(
                    psiElement().afterLeaf(psiElement().withElementType(BazelQueryTokenSets.WORDS))
                );

        @Override
        @RequiredReadAction
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            // consulo: Kotlin text.take(n).trim().removePrefix("\"").removePrefix("'")
            String text = parameters.getPosition().getText();
            int length = parameters.getOffset() - parameters.getPosition().getTextRange().getStartOffset();
            String prefix =
                StringUtil.trimStart(
                    StringUtil.trimStart(
                        text.substring(0, Math.min(length, text.length()))
                            .trim(),
                        "\""
                    ),
                    "'"
                );
            Project project = parameters.getEditor().getProject();
            if (project == null) {
                return;
            }
            List<String> targetSuggestions =
                new TargetCompletionsGenerator(project)
                    .getTargetsList(prefix);

            CompletionResultSet it = result.withPrefixMatcher(new BazelQueryPrefixMatcher(prefix));
            it.addAllElements(
                knownCommands.stream().map(command -> functionLookupElement(command, 1.0, parameters.getOffset())).toList()
            );
            it.addAllElements(
                knownOperations.stream().map(operation -> functionLookupElement(operation, 0.5, parameters.getOffset())).toList()
            );
            it.addAllElements(
                targetSuggestions.stream().map(target ->
                    functionLookupElement(
                        target,
                        target.startsWith(myStartTargetSign) &&
                            (target.endsWith(":all") || target.endsWith(":all-targets")) ? 0.1
                            : target.startsWith(myStartTargetSign) ? 0.3
                            : !target.startsWith(myStartTargetSign) &&
                            (target.endsWith(":all") || target.endsWith(":all-targets")) ? 0.2
                            : 0.4,
                        parameters.getOffset()
                    )
                ).toList()
            );
        }
    }

    private static LookupElement functionLookupElement(
        String name,
        double priority,
        int caretOffset
    ) {
        return PrioritizedLookupElement.withPriority(
            LookupElementBuilder
                .create(name)
                .withBoldness(true)
                .withIcon(BazelPluginIcons.bazel())
                .withInsertHandler((context, item) -> {
                    /*
                     * Inserting a selected suggestion with a target path using 'tab' pastes a fragment of the suggestion
                     * to the slash occurrence (corresponding to next subpackage on the path), for example, for the entered
                     * prefix "//p", selecting the suggestion "//path/to/my:target" using 'tab' will result in "//path/"
                     * and potentially shorten the list of suggestions.
                     */
                    if (context.getCompletionChar() == '\t') {
                        int startOffset = context.getStartOffset();
                        int cursorPositionInSuggestion = caretOffset - startOffset;
                        String currentText = item.getLookupString();
                        int nextSlashIndex = currentText.indexOf('/', cursorPositionInSuggestion);

                        if (nextSlashIndex > 0) {
                            String shortenedText = currentText.substring(0, Math.min(nextSlashIndex + 1, currentText.length()));
                            context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), shortenedText);
                            context.getEditor().getCaretModel().moveToOffset(context.getStartOffset() + shortenedText.length());
                            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
                        }
                    }
                    /*
                     * Regardless of whether we select a suggestion using tab or enter, if we complete the name of a function
                     * (ended with parentheses), we place the cursor in the middle of the parentheses to continue entering its
                     * arguments (in Bazel Query, all functions have at least one argument).
                     */
                    int offset = context.getEditor().getCaretModel().getOffset();
                    if (offset > 0 && context.getDocument().getCharsSequence().charAt(offset - 1) == ')') {
                        context.getEditor().getCaretModel().moveToOffset(offset - 1);
                    }
                    /*
                     * When completing the "let ... in ..." operation, we place the cursor between 'let' and 'in'.
                     */
                    if (offset > 0 && context.getDocument().getCharsSequence()
                        .subSequence(offset - 4, offset)
                        .toString().equals("  in")
                    ) {
                        context.getEditor().getCaretModel().moveToOffset(offset - 3);
                    }
                }),
            priority
        );
    }
}

class BazelQueryPrefixMatcher extends PrefixMatcher {
    BazelQueryPrefixMatcher(String prefix) {
        super(prefix);
    }

    @Override
    public boolean prefixMatches(String name) {
        return StringUtil.startsWithIgnoreCase(name, getPrefix());
    }

    @Override
    public PrefixMatcher cloneWithPrefix(String newPrefix) {
        return new BazelQueryPrefixMatcher(newPrefix);
    }
}
