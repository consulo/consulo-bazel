package org.jetbrains.bazel.languages.bazelrc.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.TokenSet;
import consulo.language.editor.completion.AutoCompletionPolicy;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.bazelrc.flags.OptionEffectTag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class BazelrcCompletionContributor extends CompletionContributor {
    public BazelrcCompletionContributor() {
        extend(CompletionType.BASIC, BazelCommandCompletionProvider.psiPattern, new BazelCommandCompletionProvider());
        extend(CompletionType.BASIC, BazelImportCompletionProvider.psiPattern, new BazelImportCompletionProvider());
        extend(CompletionType.BASIC, BazelConfigCompletionProvider.psiPattern, new BazelConfigCompletionProvider());
        extend(CompletionType.BASIC, BazelFlagCompletionProvider.psiPattern, new BazelFlagCompletionProvider());
    }

    @Override
    @RequiredReadAction
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        super.fillCompletionVariants(parameters, result);
    }

    // consulo: completion contributors are language-bound extensions (upstream XML language="Bazelrc")
    @Override
    public Language getLanguage() {
        return BazelrcLanguage.INSTANCE;
    }

    // consulo: upstream top-level declarations of BazelrcCompletionContributor.kt are static members of this class

    static final TokenSet importOrCommandTokens = TokenSet.create(BazelrcTokenTypes.COMMAND, BazelrcTokenTypes.IMPORT);

    public static class BazelCommandCompletionProvider implements CompletionProvider {
        public static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement().withLanguage(BazelrcLanguage.INSTANCE).andOr(
                psiElement().withElementType(importOrCommandTokens),
                psiElement().atStartOf(psiElement().withElementType(importOrCommandTokens))
            );

        // TODO(BAZEL-1195): Find a nicer way of building this, and the "set of correct flag with type / value for command" ?
        //    Maybe parsing the bazel help output on project sync?
        public static final Map<String, String> knownCommandsToDescriptions;

        static {
            // consulo: Kotlin mapOf keeps the insertion order
            Map<String, String> map = new LinkedHashMap<>();
            map.put("common", "flags that apply to all commands");
            map.put("analyze-profile", "Analyzes build profile data.");
            map.put("aquery", "Analyzes the given targets and queries the action graph.");
            map.put("build", "Builds the specified targets.");
            map.put("canonicalize-flags", "Canonicalizes a list of bazel options.");
            map.put("clean", "Removes output files and optionally stops the server.");
            map.put("coverage", "Generates code coverage report for specified test targets.");
            map.put("cquery", "Loads, analyzes, and queries the specified targets w/ configurations.");
            map.put("dump", "Dumps the internal state of the bazel server process.");
            map.put("fetch", "Fetches external repositories that are prerequisites to the targets.");
            map.put("help", "Prints help for commands, or the index.");
            map.put("info", "Displays runtime info about the bazel server.");
            map.put("license", "Prints the license of this software.");
            map.put("mobile-install", "Installs targets to mobile devices.");
            map.put("mod", "Queries the Bzlmod external dependency graph");
            map.put("print_action", "Prints the command line args for compiling a file.");
            map.put("query", "Executes a dependency graph query.");
            map.put("run", "Runs the specified target.");
            map.put("shutdown", "Stops the bazel server.");
            map.put("sync", "Syncs all repositories specified in the workspace file");
            map.put("test", "Builds and runs the specified test targets.");
            map.put("vendor", "Fetches external repositories into a folder specified by the flag --vendor_dir.");
            map.put("version", "Prints version information for bazel.");
            knownCommandsToDescriptions = Collections.unmodifiableMap(map);
        }

        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            result.addAllElements(
                knownCommandsToDescriptions.entrySet().stream().map(it -> functionLookupElement(it.getKey(), it.getValue())).toList()
            );
        }

        private LookupElement functionLookupElement(String name, String description) {
            return LookupElementBuilder
                .create(name)
                .withBoldness(true)
                .withTailText(" " + description)
                .withIcon(BazelPluginIcons.bazel());
        }
    }

    private static class BazelConfigCompletionProvider implements CompletionProvider {
        static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement().withLanguage(BazelrcLanguage.INSTANCE).andOr(
                psiElement(BazelrcTokenTypes.CONFIG),
                psiElement().beforeLeaf(psiElement(BazelrcTokenTypes.CONFIG))
            );

        @Override
        @RequiredReadAction
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            BazelrcLine line = PsiTreeUtil.getParentOfType(parameters.getPosition(), BazelrcLine.class);
            if (line != null) {
                BazelrcConfigDeclarationsProcessor processor = new BazelrcConfigDeclarationsProcessor(line);
                PsiFile containingFile = line.getContainingFile();
                processor.processFile(containingFile != null && containingFile.getOriginalFile() instanceof BazelrcFile file ? file : null);
                result.addAllElements(processor.getResults().keySet().stream().map(this::functionLookupElement).toList());
            }
        }

        private LookupElement functionLookupElement(String name) {
            return LookupElementBuilder.create(name).withBoldness(true).withIcon(BazelPluginIcons.bazel());
        }
    }

    public static class BazelImportCompletionProvider implements CompletionProvider {
        public static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement().withLanguage(BazelrcLanguage.INSTANCE).andOr(
                psiElement().withElementType(importOrCommandTokens),
                psiElement().atStartOf(psiElement().withElementType(importOrCommandTokens))
            );

        public static final Map<String, String> importKeywordAndDescriptions;

        static {
            // consulo: Kotlin mapOf keeps the insertion order
            Map<String, String> map = new LinkedHashMap<>();
            map.put("import", "add a required import statement");
            map.put("try-import", "adds an optional import statement");
            importKeywordAndDescriptions = Collections.unmodifiableMap(map);
        }

        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            result.addAllElements(
                importKeywordAndDescriptions.entrySet().stream().map(it ->
                    LookupElementBuilder
                        .create(it.getKey())
                        .withTailText(" " + it.getValue(), true)
                        .withIcon(BazelPluginIcons.bazelConfig())
                        .withAutoCompletionPolicy(AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE)
                ).toList()
            );
        }
    }

    static class BazelFlagCompletionProvider implements CompletionProvider {
        @Override
        @RequiredReadAction
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            BazelrcLine line = PsiTreeUtil.getParentOfType(parameters.getPosition(), BazelrcLine.class, true);
            if (line == null) {
                return;
            }
            String command = line.getCommand();
            // consulo: Kotlin Map.filterNot/filterValues keep the insertion order
            Map<String, Flag> flagsMap = new LinkedHashMap<>();
            for (Map.Entry<String, Flag> it : Flag.all().entrySet()) {
                // filter out old flag names
                if (!it.getValue().getOption().oldName().isEmpty() &&
                    it.getKey().endsWith(it.getValue().getOption().oldName())) {
                    continue;
                }
                // filter out NO_OP flags
                if (Arrays.asList(it.getValue().getOption().effectTags()).contains(OptionEffectTag.NO_OP)) {
                    continue;
                }
                // filter out values that don't apply to the current command line+
                if (!(command.equals("common") || Arrays.asList(it.getValue().getOption().commands()).contains(command))) {
                    continue;
                }
                flagsMap.put(it.getKey(), it.getValue());
            }

            PsiElement pos = parameters.getPosition();
            result.addAllElements(
                flagsMap.entrySet().stream().map(it -> {
                    String k = it.getKey();
                    BazelFlagSymbol flagSymbol = new BazelFlagSymbol(it.getValue(), pos.getProject());
                    return LookupElementBuilder
                        .create(flagSymbol, k)
                        .withLookupString(k + "_xxx")
                        .withTypeText(" " + flagSymbol.getFlag().getOption().valueHelp())
                        .withPresentableText(k)
                        .withInsertHandler((ctx, item) -> {
                            PsiElement psiElement = ctx.getFile().findElementAt(ctx.getStartOffset());
                            if (psiElement != null) {
                                if (!psiElement.getText().equals(k)) {
                                    ctx.getEditor().getDocument().replaceString(
                                        psiElement.getTextOffset(),
                                        psiElement.getTextRange().getEndOffset(),
                                        k
                                    );
                                }
                            }
                        });
                }).toList()
            );
        }

        static final PsiElementPattern.Capture<PsiElement> psiPattern =
            psiElement()
                .withElementType(TokenSet.create(BazelrcTokenTypes.VALUE, BazelrcTokenTypes.FLAG))
                .atStartOf(psiElement(BazelrcFlag.class))
                .withTreeParent(
                    psiElement(BazelrcFlag.class)
                );
    }

    public static <T> T letIf(T receiver, boolean predicate, Function<T, T> block) {
        if (predicate) {
            return block.apply(receiver);
        }
        else {
            return receiver;
        }
    }
}
