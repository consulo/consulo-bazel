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
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.Environment;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkStarArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider.StarlarkCallableFunctionInfoProvider;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider.StarlarkFuncParam;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider.StarlarkFunctionInfo;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFunctionInfoProvider.StarlarkGlobalFunctionInfoProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@ExtensionImpl
public class StarlarkFunctionCallValidationInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new CallArgumentValidationVisitor(holder);
    }

    private static class CallArgumentValidationVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        private final List<StarlarkFunctionInfoProvider> mySignatureProviders = List.of(
            new StarlarkGlobalFunctionInfoProvider(),
            new StarlarkCallableFunctionInfoProvider()
        );

        CallArgumentValidationVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitCallExpression(StarlarkCallExpression node) {
            StarlarkArgumentList argumentList = node.getArgumentList();
            if (argumentList == null) {
                return;
            }
            StarlarkArgumentElement[] arguments = argumentList.getArguments();
            validateOrder(arguments);

            StarlarkFunctionInfo resolved = null;
            for (StarlarkFunctionInfoProvider it : mySignatureProviders) {
                resolved = it.resolve(node);
                if (resolved != null) {
                    break;
                }
            }
            if (resolved == null) {
                return;
            }
            if (!validateEnvironment(node, resolved.allowedEnvironments())) {
                return;
            }
            validateAgainstSignature(node, arguments, resolved.params());
            checkDependencyOverrideResolution(node);
        }

        private boolean validateEnvironment(StarlarkCallExpression call, @Nullable Set<Environment> allowedEnvironments) {
            if (allowedEnvironments == null) {
                return true;
            }

            String fileName = Objects.requireNonNull(call.getContainingFile()).getName();
            Environment currentEnvironment;
            if (fileName.equals(Constants.MODULE_BAZEL_FILE_NAME)) {
                currentEnvironment = Environment.MODULE;
            }
            else if (ArrayUtil.contains(fileName, Constants.BUILD_FILE_NAMES)) {
                currentEnvironment = Environment.BUILD;
            }
            else if (ArrayUtil.contains(fileName, Constants.WORKSPACE_FILE_NAMES)) {
                currentEnvironment = Environment.BUILD;
            }
            else if (fileName.equals("REPO.bazel")) {
                currentEnvironment = Environment.REPO;
            }
            else if (fileName.equals("VENDOR.bazel")) {
                currentEnvironment = Environment.VENDOR;
            }
            else {
                currentEnvironment = fileName.endsWith(".bzl") ? Environment.BZL : null;
            }
            if (currentEnvironment == null) {
                return true;
            }
            if (allowedEnvironments.contains(currentEnvironment)) {
                return true;
            }

            PsiElement firstChild = call.getFirstChild();
            myHolder.newProblem(
                StarlarkLocalize.inspectionDescriptionCallNotAvailableInEnvironment(
                    firstChild != null ? firstChild.getText() : "<call>",
                    currentEnvironment.name(),
                    allowedEnvironments.stream().map(it -> it.name()).sorted().collect(Collectors.joining(", "))
                )
            ).range(firstChild != null ? firstChild : call).create();
            return false;
        }

        private void checkDependencyOverrideResolution(StarlarkCallExpression call) {
            PsiElement firstChild = call.getFirstChild();
            if (firstChild == null) {
                return;
            }
            String functionName = firstChild.getText();
            if (!functionName.equals("git_override") && !functionName.equals("archive_override")) {
                return;
            }
            PsiElement calledExpression = call.getCalledExpression();
            PsiReference reference = calledExpression == null ? null : calledExpression.getReference();
            if (reference == null) {
                return;
            }
            if (reference.resolve() != null) {
                return;
            }
            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionCallOverrideMissingDep())
                .range(firstChild)
                .create();
        }

        private void validateOrder(StarlarkArgumentElement[] arguments) {
            boolean seenNamed = false;
            boolean seenStar = false;
            boolean seenKwStar = false;

            for (StarlarkArgumentElement arg : arguments) {
                if (arg instanceof StarlarkArgumentExpression) {
                    if (seenNamed || seenStar || seenKwStar) {
                        registerProblem(arg, StarlarkLocalize.inspectionDescriptionCallPositionalAfterKeywordOrStar());
                    }
                }
                else if (arg instanceof StarlarkNamedArgumentExpression) {
                    if (seenKwStar) {
                        registerProblem(arg, StarlarkLocalize.inspectionDescriptionCallArgumentAfterKwargs());
                    }
                    seenNamed = true;
                }
                else if (arg instanceof StarlarkStarArgumentExpression starArg) {
                    if (isKwStar(starArg)) {
                        if (seenKwStar) {
                            registerProblem(arg, StarlarkLocalize.inspectionDescriptionCallMultipleKwargsUnpacking());
                        }
                        seenKwStar = true;
                    }
                    else {
                        if (seenKwStar) {
                            registerProblem(arg, StarlarkLocalize.inspectionDescriptionCallStarAfterKwargs());
                        }
                        seenStar = true;
                    }
                }
            }
        }

        private void validateAgainstSignature(
            StarlarkCallExpression call,
            StarlarkArgumentElement[] arguments,
            List<StarlarkFuncParam> params
        ) {
            ListIterator<StarlarkFuncParam> expectedParamIter = params.stream().filter(StarlarkFuncParam::positional).toList().listIterator();
            StarlarkFuncParam expectedParam = nextOrNull(expectedParamIter);
            boolean onlyKeywordArgsExpected = false;
            boolean hasUnknownPositional = false;
            boolean hasUnknownKeywords = false;
            boolean kwArgsFound = false;

            Set<String> matchedArguments = new LinkedHashSet<>();
            Set<String> matchedByPositional = new HashSet<>();
            boolean acceptsKwArgs = params.stream().anyMatch(StarlarkFuncParam::isKwArgs);

            for (StarlarkArgumentElement arg : arguments) {
                if (arg instanceof StarlarkNamedArgumentExpression namedArg) {
                    String argName = namedArg.getName();
                    if (argName == null) {
                        continue;
                    }
                    StarlarkFuncParam matched = params.stream().filter(it -> it.name().equals(argName)).findFirst().orElse(null);

                    if (matched == null && !acceptsKwArgs) {
                        registerProblem(
                            Objects.requireNonNull(namedArg.getFirstChild()),
                            StarlarkLocalize.inspectionDescriptionCallNamedParameterNotFound(argName)
                        );
                    }
                    else if (matchedArguments.contains(argName)) {
                        registerProblem(
                            namedArg,
                            matchedByPositional.contains(argName)
                                ? StarlarkLocalize.inspectionDescriptionCallMultipleValuesForParameter(argName)
                                : StarlarkLocalize.inspectionDescriptionCallDuplicateNamedArgument(argName)
                        );
                    }
                    else if (matched != null && !matched.named()) {
                        registerProblem(namedArg, StarlarkLocalize.inspectionDescriptionCallUnnamedArgWithName(argName));
                    }
                    else {
                        matchedArguments.add(argName);
                        if (matched == null) {
                            kwArgsFound = true;
                        }
                    }

                    onlyKeywordArgsExpected = true;
                }
                else if (arg instanceof StarlarkArgumentExpression) {
                    if (onlyKeywordArgsExpected) {
                        continue;
                    }

                    if (expectedParam == null) {
                        registerProblem(arg, StarlarkLocalize.inspectionDescriptionCallTooManyPositional());
                        continue;
                    }

                    if (!matchedArguments.contains(expectedParam.name())) {
                        matchedArguments.add(expectedParam.name());
                        matchedByPositional.add(expectedParam.name());
                    }
                    if (!expectedParam.isVarArgs()) {
                        expectedParam = nextOrNull(expectedParamIter);
                    }
                }
                else if (arg instanceof StarlarkStarArgumentExpression starArg) {
                    onlyKeywordArgsExpected = true;
                    if (isKwStar(starArg)) {
                        hasUnknownKeywords = true;
                    }
                    else {
                        hasUnknownPositional = true;
                    }
                }
            }

            List<String> missingArguments = new ArrayList<>();
            for (StarlarkFuncParam param : params) {
                if (!param.required()) {
                    continue;
                }
                if (matchedArguments.contains(param.name())) {
                    continue;
                }
                if (param.isKwArgs() && kwArgsFound) {
                    continue;
                }
                if (param.positional() && hasUnknownPositional) {
                    continue;
                }
                if (param.named() && hasUnknownKeywords) {
                    continue;
                }

                missingArguments.add(param.name());
            }
            if (!missingArguments.isEmpty()) {
                registerProblem(
                    Objects.requireNonNull(call.getFirstChild()), // only annotate the function name
                    StarlarkLocalize.inspectionDescriptionCallMissingRequiredArgument(String.join(", ", missingArguments))
                );
            }
        }

        private boolean isKwStar(StarlarkStarArgumentExpression arg) {
            return arg.getText().stripLeading().startsWith("**");
        }

        private static <T> @Nullable T nextOrNull(ListIterator<T> receiver) {
            return receiver.hasNext() ? receiver.next() : null;
        }

        // consulo: holder.registerProblem(element, message) -> ProblemsHolder.newProblem(LocalizeValue) builder
        private void registerProblem(PsiElement element, LocalizeValue message) {
            myHolder.newProblem(message).range(element).create();
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkFunctionCallValidation";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkFunctionCallValidationCheck();
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
