package org.jetbrains.bazel.languages.bazelrc.documentation;

import consulo.application.util.HtmlChunk;
import consulo.bazel.documentation.BazelMarkdown;
import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.bazelrc.flags.Option;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BazelFlagDocumentationTarget implements DocumentationTarget {
    // consulo: upstream top-level declarations of BazelFlagDocumentationTarget.kt are static members of this class

    private static final Pattern RE_FLAG = Pattern.compile("(?<=\\s)(--[\\S=]+)(?=[\\s$.])");

    // match `<space>'--xxadsfas'<space or end of word>`
    private static final Pattern RE_FLAG_QUOTED = Pattern.compile("(?<=\\s)('--['\\S=]+')(?=[\\s$.])");

    // this will take the description and massage it into a better markdown variant so that the transformed HTML is nicer
    private static String help(Flag receiver) {
        String text = trimIndent(receiver.getOption().help());
        // -> _--asdfasd_ -> _`--asdfasd`_
        text = RE_FLAG.matcher(text).replaceAll(it -> Matcher.quoteReplacement("`" + it.group(1) + "`"));
        // -> _'--asdfasd_' -> _`--asdfasd`_
        text = RE_FLAG_QUOTED.matcher(text).replaceAll(it -> Matcher.quoteReplacement("`" + it.group(1) + "`"));
        return text;
    }

    private static String title(Flag receiver) {
        return receiver instanceof Flag.Boolean
            ? "[no]" + receiver.getOption().name()
            : receiver.getOption().name();
    }

    private static String expandsTo(Flag receiver) {
        Option option = receiver.getOption();
        if (option.expandsTo().length == 0) {
            return "";
        }
        return trimMargin(
            """
                |**Expands to**:
                |```commandline
                |%s
                |```
                """.formatted(String.join(" ", option.expandsTo()))
        );
    }

    private static String type(Flag receiver) {
        String valueHelp = receiver.getOption().valueHelp();
        if (valueHelp.isEmpty()) {
            return "";
        }
        return "**type**" + "\n" + ": `" + valueHelp + "`";
    }

    // consulo: upstream Flag.default(); "default" is a Java keyword
    private static String defaultValue(Flag receiver) {
        String defaultValue = receiver.getOption().defaultValue();
        if (defaultValue.isEmpty()) {
            return "";
        }
        String it = defaultValue.replace("\n+", "");
        return "**default**" + "\n" + ": `" + it + "`";
    }

    private static String oldName(Flag receiver) {
        String oldName = receiver.getOption().oldName();
        if (oldName.isEmpty()) {
            return "";
        }
        return "**old name**" + "\n" + ": `" + oldName + "`";
    }

    private static String allowMultiple(Flag receiver) {
        if (!receiver.getOption().allowMultiple()) {
            return "";
        }
        return "**can be used multiples times**";
    }

    private static String effects(Flag receiver) {
        Option option = receiver.getOption();
        if (option.effectTags().length == 0) {
            return "";
        }
        String it = Arrays.stream(option.effectTags())
            .map(tag -> "_" + tag.name().toLowerCase(Locale.ROOT) + "_")
            .collect(Collectors.joining(", "));
        return "**Effects**: " + it;
    }

    private static String metadataTags(Flag receiver) {
        Option option = receiver.getOption();
        if (option.metadataTags().length == 0) {
            return "";
        }
        String it = Arrays.stream(option.metadataTags())
            .map(tag -> "_" + tag.name().toLowerCase(Locale.ROOT) + "_")
            .collect(Collectors.joining(", "));
        return "**Metadata tags**: " + it;
    }

    private static String commands(Flag receiver) {
        Option option = receiver.getOption();
        if (option.commands().length == 0) {
            return "";
        }
        String it = Arrays.stream(option.commands())
            .map(command -> command.toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
        return "**Commands**: `" + it + "`";
    }

    public static String flagToDocumentationMarkdownText(Flag flag) {
        String markdownText =
            trimMargin(
                """
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    |
                    |%s
                    """.formatted(
                    type(flag),
                    defaultValue(flag),
                    oldName(flag),
                    allowMultiple(flag),
                    help(flag),
                    expandsTo(flag),
                    effects(flag),
                    metadataTags(flag),
                    commands(flag)
                )
            );
        return markdownText;
    }

    private static DocumentationResult.Documentation getDocumentationResult(Flag flag, Project project) {
        String markdownText = flagToDocumentationMarkdownText(flag);

        // consulo: DocMarkdownToHtmlConverter.convert(project, markdown) -> BazelMarkdown.toHtml(markdown)
        HtmlChunk body = HtmlChunk.raw(BazelMarkdown.toHtml(markdownText));
        HtmlChunk fragment =
            HtmlChunk.fragment(
                HtmlChunk.text(title(flag)).bold().wrapWith(HtmlChunk.head()),
                body.wrapWith(HtmlChunk.body())
            );

        return DocumentationResult.documentation(fragment.wrapWith(HtmlChunk.html()).toString());
    }

    // consulo: upstream symbol pointer; BazelFlagSymbol was its own pointer, so the symbol is kept directly
    private final BazelFlagSymbol mySymbolPtr;

    public BazelFlagDocumentationTarget(BazelFlagSymbol symbol) {
        mySymbolPtr = symbol;
    }

    public BazelFlagSymbol getSymbolPtr() {
        return mySymbolPtr;
    }

    // consulo: createPointer(), dereference() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public DocumentationResult computeDocumentation() {
        BazelFlagSymbol symbol = mySymbolPtr;
        return DocumentationResult.asyncDocumentation(() -> getDocumentationResult(symbol.getFlag(), symbol.getProject()));
    }

    // consulo: upstream internal top-level class, used from org.jetbrains.bazel.languages.projectview.documentation
    public static class BazelFlagSymbolLessDocumentationTarget implements DocumentationTarget {
        private final PsiElement myElement;
        private final @Nullable PsiElement myOriginalElement;
        private final Flag myFlag;

        public BazelFlagSymbolLessDocumentationTarget(PsiElement element, @Nullable PsiElement originalElement, Flag flag) {
            myElement = element;
            myOriginalElement = originalElement;
            myFlag = flag;
        }

        public PsiElement getElement() {
            return myElement;
        }

        // consulo: createPointer() and computePresentation() are dropped,
        // pointer/presentation are not used by the classic documentation provider

        @Override
        public DocumentationResult computeDocumentation() {
            return DocumentationResult.asyncDocumentation(() -> getDocumentationResult(myFlag, myElement.getProject()));
        }
    }

    // consulo: Kotlin String.trimIndent(), String.trimMargin() and String.lines() used above

    private static List<String> lines(String text) {
        return List.of(text.split("\r\n|\n|\r", -1));
    }

    private static String trimIndent(String text) {
        List<String> lines = lines(text);
        int minCommonIndent = lines.stream()
            .filter(it -> !it.isBlank())
            .mapToInt(BazelFlagDocumentationTarget::indentWidth)
            .min()
            .orElse(0);
        return reindent(lines, line -> line.substring(Math.min(minCommonIndent, line.length())));
    }

    private static String trimMargin(String text) {
        return reindent(lines(text), line -> {
            int firstNonWhitespaceIndex = indexOfFirstNonWhitespace(line);
            if (firstNonWhitespaceIndex == -1) {
                return null;
            }
            if (line.startsWith("|", firstNonWhitespaceIndex)) {
                return line.substring(firstNonWhitespaceIndex + 1);
            }
            return null;
        });
    }

    private static String reindent(List<String> lines, Function<String, @Nullable String> indentCutFunction) {
        int lastIndex = lines.size() - 1;
        List<String> result = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String value = lines.get(index);
            if ((index == 0 || index == lastIndex) && value.isBlank()) {
                continue;
            }
            String cut = indentCutFunction.apply(value);
            result.add(cut != null ? cut : value);
        }
        return String.join("\n", result);
    }

    private static int indentWidth(String line) {
        int index = indexOfFirstNonWhitespace(line);
        return index == -1 ? line.length() : index;
    }

    private static int indexOfFirstNonWhitespace(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
