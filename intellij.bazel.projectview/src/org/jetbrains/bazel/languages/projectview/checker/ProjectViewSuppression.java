package org.jetbrains.bazel.languages.projectview.checker;

import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A suppression comment, with its {@code ids} and the {@code idsRange} of its ID list in the file.
 * <p>
 * A `# suppress: <id>, <id> <free text>` comment covers the element below it, up to a blank line.
 * It has the indentation of that element.
 * The ID list ends at the first character that is not a part of an ID, a comma or a white space.
 */
public record ProjectViewSuppression(
    PsiComment comment,
    List<ProjectViewProblem.Id> ids,
    TextRange idsRange
) {
    private static final String ID_PATTERN = ProjectViewProblem.Id.PATTERN;

    // companion object

    private static final String DIRECTIVE_PREFIX = "# suppress: ";

    private static final Pattern directivePattern =
        Pattern.compile("^#\\s*suppress:\\s*(" + ID_PATTERN + "(?:\\s*,\\s*" + ID_PATTERN + ")*)");

    /**
     * The suppressions that belong to {@code element}, the nearest first.
     */
    public static Stream<ProjectViewSuppression> from(PsiElement element) {
        return precedingComments(element).stream().map(ProjectViewSuppression::parse).filter(Objects::nonNull);
    }

    /**
     * The text of a suppression comment that suppresses {@code ids}.
     */
    public static String commentText(List<ProjectViewProblem.Id> ids) {
        return DIRECTIVE_PREFIX + commentIdsText(ids);
    }

    /**
     * The ID list for {@code ids}, as it stands in a suppression comment.
     */
    public static String commentIdsText(List<ProjectViewProblem.Id> ids) {
        return ids.stream().map(ProjectViewProblem.Id::value).collect(Collectors.joining(", "));
    }

    private static @Nullable ProjectViewSuppression parse(PsiComment comment) {
        Matcher matcher = directivePattern.matcher(comment.getText());
        if (!matcher.find() || matcher.group(1) == null) {
            return null;
        }
        String idList = matcher.group(1);
        return new ProjectViewSuppression(
            comment,
            Arrays.stream(idList.split(",", -1))
                .map(it -> new ProjectViewProblem.Id(it.trim()))
                .toList(),
            new TextRange(matcher.start(1), matcher.end(1)).shiftRight(comment.getTextRange().getStartOffset())
        );
    }

    // consulo: upstream internal top-level declarations of ProjectViewSuppression.kt

    public static boolean isSuppressible(ProjectViewProblem receiver) {
        return receiver.severity().compareTo(ProjectViewProblem.Severity.Error) < 0;
    }

    public static Stream<ProjectViewProblem> withSuppressions(Stream<ProjectViewProblem> receiver) {
        return receiver.filter(it -> !(isSuppressible(it) && isSuppressed(it)));
    }

    private static boolean isSuppressed(ProjectViewProblem receiver) {
        PsiElement element = receiver.element();
        if (element == null) {
            return false;
        }
        // consulo: Kotlin element.parents(withSelf = true).takeWhile { it !is PsiFile }
        for (PsiElement it = element; it != null && !(it instanceof PsiFile); it = it.getParent()) {
            if (from(it).anyMatch(suppression -> suppression.ids().contains(receiver.id()))) {
                return true;
            }
        }
        return false;
    }

    // consulo: Kotlin sequence {} builder, collected eagerly
    private static List<PsiComment> precedingComments(PsiElement receiver) {
        List<PsiComment> result = new ArrayList<>();
        PsiElement firstLeaf = PsiTreeUtil.getDeepestFirst(receiver);
        boolean indented = isIndented(firstLeaf);
        int newlines = 0;
        for (PsiElement leaf = PsiTreeUtil.prevLeaf(firstLeaf); leaf != null; leaf = PsiTreeUtil.prevLeaf(leaf)) {
            IElementType elementType = PsiUtilCore.getElementType(leaf);
            if (leaf instanceof PsiComment comment) {
                if (isIndented(comment) != indented) {
                    return result;
                }
                result.add(comment);
                newlines = 0;
            }
            else if (elementType == ProjectViewTokenType.NEWLINE) {
                if (++newlines > 1) {
                    return result;
                }
            }
            else if (leaf instanceof PsiWhiteSpace || elementType == ProjectViewTokenType.INDENT) {
                // Unit
            }
            else {
                return result;
            }
        }
        return result;
    }

    private static boolean isIndented(PsiElement receiver) {
        PsiElement previous = PsiTreeUtil.prevLeaf(receiver);
        if (previous == null) {
            return false;
        }
        return PsiUtilCore.getElementType(previous) != ProjectViewTokenType.NEWLINE;
    }
}
