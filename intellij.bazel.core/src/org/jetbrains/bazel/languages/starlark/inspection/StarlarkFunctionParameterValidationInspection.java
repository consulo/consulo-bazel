package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class StarlarkFunctionParameterValidationInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new ParameterValidationVisitor(holder);
    }

    private static class ParameterValidationVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        ParameterValidationVisitor(
            ProblemsHolder holder
        ) {
            myHolder = holder;
        }

        @Override
        public void visitFunctionDeclaration(StarlarkFunctionDeclaration node) {
            validateCallable(node);
        }

        @Override
        public void visitLambdaExpression(StarlarkLambdaExpression node) {
            validateCallable(node);
        }

        private void validateCallable(StarlarkCallable callable) {
            StarlarkParameterList parameterList = PsiTreeUtil.getChildOfType(callable, StarlarkParameterList.class);
            if (parameterList == null) {
                return;
            }
            List<PsiElement> signatureItems = Arrays.stream(parameterList.getChildren())
                .filter(it -> it instanceof StarlarkParameter || it instanceof StarlarkKeywordOnlyBoundary)
                .toList();
            if (signatureItems.isEmpty()) {
                return;
            }

            Set<String> seenNames = new HashSet<>();
            ParameterState state = ParameterState.BEFORE_OPTIONAL;
            boolean boundarySeen = false;

            for (int index = 0; index < signatureItems.size(); index++) {
                PsiElement item = signatureItems.get(index);
                if (state == ParameterState.AFTER_KEYWORD_VARIADIC) {
                    myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersAfterKeywordVariadic())
                        .range(item)
                        .create();
                    continue;
                }

                if (item instanceof StarlarkParameter parameter) {
                    checkDuplicateName(parameter, seenNames);

                    if (parameter instanceof StarlarkMandatoryParameter) {
                        if (state == ParameterState.AFTER_OPTIONAL) {
                            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersMandatoryAfterOptional())
                                .range(item)
                                .create();
                        }
                    }
                    else if (parameter instanceof StarlarkOptionalParameter) {
                        if (state == ParameterState.BEFORE_OPTIONAL) {
                            state = ParameterState.AFTER_OPTIONAL;
                        }
                    }
                    else if (parameter instanceof StarlarkVariadicParameter) {
                        if (state == ParameterState.AFTER_VARIADIC) {
                            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersMultipleVariadic())
                                .range(item)
                                .create();
                        }
                        else {
                            state = ParameterState.AFTER_VARIADIC;
                        }
                    }
                    else if (parameter instanceof StarlarkKeywordVariadicParameter) {
                        state = ParameterState.AFTER_KEYWORD_VARIADIC;
                    }
                }
                else if (item instanceof StarlarkKeywordOnlyBoundary) {
                    if (boundarySeen || state == ParameterState.AFTER_VARIADIC) {
                        myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersMultipleVariadic())
                            .range(item)
                            .create();
                        continue;
                    }

                    boundarySeen = true;
                    state = ParameterState.AFTER_VARIADIC;

                    PsiElement nextItem = index + 1 < signatureItems.size() ? signatureItems.get(index + 1) : null;
                    boolean isValidFollower = nextItem instanceof StarlarkMandatoryParameter || nextItem instanceof StarlarkOptionalParameter;
                    if (!isValidFollower) {
                        myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersBareAsterisk())
                            .range(item)
                            .create();
                    }
                }
            }
        }

        private void checkDuplicateName(StarlarkParameter parameter, Set<String> seenNames) {
            String name = parameter.getName();
            if (name == null) {
                return;
            }
            if (!seenNames.add(name)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionParametersDuplicateName(name))
                    .range(parameter)
                    .create();
            }
        }
    }

    private enum ParameterState {
        BEFORE_OPTIONAL,
        AFTER_OPTIONAL,
        AFTER_VARIADIC,
        AFTER_KEYWORD_VARIADIC,
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkFunctionParameterValidation";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkFunctionParameterValidationCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.starlarkInspectionGroupName();
    }

    // consulo: upstream groupPath="Bazel"
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{BazelPluginLocalize.bazelInspectionGroupName(), getGroupDisplayName()};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
