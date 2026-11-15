package org.jetbrains.bazel.languages.starlark.quickFixes;

import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static org.jetbrains.bazel.languages.starlark.StarlarkConstants.ALLOW_EMPTY_KEYWORD;

// consulo: the ModCommand API (PsiUpdateModCommandAction) does not exist in Consulo; a classic quick fix / intention action
// bound to the glob expression (smart pointer), run in a write action
public class StarlarkGlobAllowEmptyQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    public StarlarkGlobAllowEmptyQuickFix(StarlarkGlobExpression callExpression) {
        super(callExpression);
    }

    // consulo: Consulo intention actions have no family name; kept from upstream
    public LocalizeValue getFamilyName() {
        return StarlarkLocalize.quickfixGlobAllowEmpty();
    }

    // consulo: upstream PsiUpdateModCommandAction used the family name as the text
    @Override
    public LocalizeValue getText() {
        return getFamilyName();
    }

    // consulo: upstream invoke(actionContext: ActionContext, element: StarlarkGlobExpression, updater: ModPsiUpdater);
    // the classic intention edits the physical PSI, so updater.getWritable(x) is x itself
    @Override
    public void invoke(
        Project project,
        PsiFile file,
        @Nullable Editor editor,
        PsiElement startElement,
        PsiElement endElement
    ) {
        if (!(startElement instanceof StarlarkGlobExpression element)) {
            return;
        }
        StarlarkGlobExpression writableElement = element;
        StarlarkNamedArgumentExpression allowEmptyArgument = writableElement.getKeywordArgument(ALLOW_EMPTY_KEYWORD);
        PsiElement newArgument =
            createFromText(
                writableElement.getProject(),
                ALLOW_EMPTY_KEYWORD + " = " + titlecaseFirstChar(Boolean.toString(true))
            );

        if (allowEmptyArgument != null) {
            allowEmptyArgument.replace(newArgument);
        }
        else {
            addAllowEmptyArgument(writableElement, newArgument);
        }
    }

    // consulo: Kotlin replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
    private static String titlecaseFirstChar(String value) {
        if (value.isEmpty() || !Character.isLowerCase(value.charAt(0))) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
    }

    private void addAllowEmptyArgument(
        StarlarkGlobExpression element,
        PsiElement newArgument
    ) {
        Project project = element.getProject();
        StarlarkArgumentElement[] argumentList = element.getArguments();
        StarlarkArgumentElement lastArgument = argumentList.length == 0 ? null : argumentList[argumentList.length - 1];

        if (lastArgument != null) {
            PsiElement writableLastArgument = lastArgument;
            PsiElement parent = Objects.requireNonNull(writableLastArgument.getParent());
            PsiElement anchor = writableLastArgument;

            // add comma if needed
            if (!writableLastArgument.getText().stripTrailing().endsWith(",")) {
                anchor = parent.addAfter(createFromText(project, ","), anchor);
            }

            // add allow_empty argument separated by space
            PsiElement addedArgument = parent.addAfter(newArgument, anchor);
            parent.addBefore(createFromText(project, " "), addedArgument);
        }
        else { // empty argument list
            StarlarkGlobExpression writableElement = element;

            // delete empty parentheses if needed
            PsiElement next = writableElement.getNextSibling();
            if (next != null && StringUtil.equalsIgnoreWhitespaces(next.getText(), "()")) {
                next.delete();
            }

            // add allow_empty argument enclosed in parentheses
            PsiElement addedArgument = writableElement.addAfter(newArgument, writableElement.getFirstChild());
            writableElement.addBefore(createFromText(project, "("), addedArgument);
            writableElement.addAfter(createFromText(project, ")"), addedArgument);
        }
    }

    private PsiElement createFromText(Project project, String text) {
        PsiFile file =
            PsiFileFactory.getInstance(project).createFileFromText(
                "dummy.starlark",
                StarlarkLanguage.INSTANCE,
                text
            );
        return Objects.requireNonNull(file.getFirstChild());
    }
}
