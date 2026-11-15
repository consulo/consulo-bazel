package org.jetbrains.bazel.ui.console;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.execution.ui.console.ConsoleFilterProvider;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BazelBuildTargetConsoleFilter implements Filter {
    private final Project myProject;

    private final String myHighlightGroupName = "highlightGroup";

    // The set of characters allowed in labels is taken from https://bazel.build/concepts/labels#target-names
    // consulo: Kotlin raw string with $highlightGroupName interpolation -> text block (backslashes doubled) with replace();
    // trimMargin() only drops the trailing blank line here
    private final Pattern myBazelTargetRegex =
        Pattern.compile(
            """
                (^|\\W)
                        (?<$highlightGroupName>
                        ((@|@@)([a-zA-Z0-9!%\\-^_"&'()*+,;<=>?\\[\\]{|}~/.\\#]*))? #@ or @@ with potential external repo names
                        //([a-zA-Z0-9!%\\-@^_"&'()*+,;<=>?\\[\\]{|}~/.\\#]*):      #path
                        ([a-zA-Z0-9!%\\-@^_"&'()*+,;<=>?\\[\\]{|}~/.\\#]+)         #target
                        )""".replace("$highlightGroupName", myHighlightGroupName),
            Pattern.COMMENTS | Pattern.MULTILINE
        );

    public BazelBuildTargetConsoleFilter(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public @Nullable Result applyFilter(String line, int entireLength) {
        List<ResultItem> results = new ArrayList<>();
        Matcher matcher = myBazelTargetRegex.matcher(line);
        while (matcher.find()) {
            Result result = toFilterResultOrNull(matcher, line, entireLength);
            if (result != null) {
                results.add(result);
            }
        }
        return new Result(results);
    }

    @RequiredReadAction
    private @Nullable Result toFilterResultOrNull(Matcher match, String line, int entireLength) {
        String highlightGroup = match.group(myHighlightGroupName);
        if (highlightGroup == null) {
            return null;
        }
        if (!(Label.parseOrNull(highlightGroup) instanceof Label.ResolvedLabel label)) {
            return null;
        }
        PsiElement psi = LabelResolveUtils.resolveLabel(myProject, label);
        if (psi == null) {
            return null;
        }
        PsiFile containingFile = psi.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(containingFile);
        if (document == null) {
            return null;
        }
        int textOffset = psi.getTextOffset();
        int documentLine = document.getLineNumber(textOffset);
        int documentColumn = textOffset - document.getLineStartOffset(documentLine);
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        OpenFileHyperlinkInfo hyperLinkInfo = new OpenFileHyperlinkInfo(myProject, virtualFile, documentLine, documentColumn);
        int highlightStartOffset = entireLength - line.length() + match.start(myHighlightGroupName);
        int highlightEndOffset = entireLength - line.length() + match.end(myHighlightGroupName);
        return new Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo);
    }
}

@ExtensionImpl
class BazelBuildTargetConsoleFilterProvider implements ConsoleFilterProvider {
    public BazelBuildTargetConsoleFilterProvider() {
    }

    @Override
    public Filter[] getDefaultFilters(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project)
            ? new Filter[]{new BazelBuildTargetConsoleFilter(project)}
            : Filter.EMPTY_ARRAY;
    }
}
