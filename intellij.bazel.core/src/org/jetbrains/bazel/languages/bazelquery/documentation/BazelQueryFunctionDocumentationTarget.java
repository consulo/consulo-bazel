package org.jetbrains.bazel.languages.bazelquery.documentation;

import consulo.application.util.HtmlChunk;
import consulo.bazel.documentation.BazelMarkdown;
import consulo.bazel.documentation.DocumentationResult;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction;
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunctionSymbol;
import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;

public class BazelQueryFunctionDocumentationTarget implements DocumentationTarget {
    private final Project myProject;
    private final BazelQueryFunction myFunction;

    public BazelQueryFunctionDocumentationTarget(BazelQueryFunctionSymbol symbol, Project project) {
        myProject = project;
        myFunction = symbol.getFunction();
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: createPointer(), dereference() and computePresentation() are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public @Nullable DocumentationResult computeDocumentation() {
        return DocumentationResult.asyncDocumentation(() -> {
            String markdownText = functionToDocumentationMarkdownText(myFunction);

            // consulo: DocMarkdownToHtmlConverter.convert(project, markdown) -> BazelMarkdown.toHtml(markdown)
            HtmlChunk body =
                HtmlChunk.raw(
                    BazelMarkdown.toHtml(markdownText)
                );
            HtmlChunk fragment =
                HtmlChunk.fragment(
                    HtmlChunk.text(myFunction.getName()).bold().wrapWith(HtmlChunk.head()),
                    body.wrapWith(HtmlChunk.body())
                );

            return DocumentationResult.documentation(fragment.wrapWith(HtmlChunk.html()).toString());
        });
    }

    public static String functionToDocumentationMarkdownText(BazelQueryFunction function) {
        // consulo: the result of the upstream Kotlin raw string with trimMargin(); the lines without a margin keep their indent
        String markdownText =
            function.getDescription() + "\n" +
                "\n" +
                "**Arguments:**\n" +
                argumentsMarkdown(function) + "\n" +
                "\n" +
                "**Example Usage:**\n" +
                "          ```\n" +
                "            " + (function instanceof BazelQueryFunction.SimpleFunction ? function.getExampleUsage() : "N/A") + "\n" +
                "          ```";
        return markdownText;
    }

    private static String argumentsMarkdown(BazelQueryFunction receiver) {
        return receiver.getArguments()
            .stream()
            .map(arg -> "- `" + arg.name() + "` (" + arg.type() + (arg.optional() ? ", optional" : "") + "): " + arg.description())
            .collect(Collectors.joining("\n"));
    }
}
