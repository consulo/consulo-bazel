package org.jetbrains.bazel.languages.starlark.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunctionParameter;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.Environment;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public interface StarlarkFunctionInfoProvider {
    @Nullable StarlarkFunctionInfo resolve(StarlarkCallExpression call);

    // consulo: upstream top-level data class of the same Kotlin file
    record StarlarkFunctionInfo(
        List<StarlarkFuncParam> params,
        @Nullable Set<Environment> allowedEnvironments
    ) {
        // consulo: upstream default argument (allowedEnvironments = null)
        public StarlarkFunctionInfo(List<StarlarkFuncParam> params) {
            this(params, null);
        }
    }

    // consulo: upstream top-level data class of the same Kotlin file
    record StarlarkFuncParam(
        String name,
        boolean positional,
        boolean named,
        boolean required,
        boolean isVarArgs,
        boolean isKwArgs
    ) {
    }


    // consulo: upstream top-level class of the same Kotlin file
    class StarlarkGlobalFunctionInfoProvider implements StarlarkFunctionInfoProvider {
        @Override
        public @Nullable StarlarkFunctionInfo resolve(StarlarkCallExpression call) {
            PsiElement parent = call.getParent();
            if (!((parent == null ? null : parent.getParent()) instanceof StarlarkFile)) {
                return null;
            }
            if (resolvesToValidTarget(call)) {
                return null;
            }

            PsiElement firstChild = call.getFirstChild();
            String functionName = firstChild == null ? null : firstChild.getText();
            if (functionName == null) {
                return null;
            }
            BazelGlobalFunction function = BazelGlobalFunctions.getFunctionByName(functionName, call.getProject());
            if (function == null) {
                return null;
            }
            List<StarlarkFuncParam> params = new ArrayList<>();
            for (BazelGlobalFunctionParameter it : function.params()) {
                params.add(new StarlarkFuncParam(
                    it.name(),
                    it.positional(),
                    it.named(),
                    it.required(),
                    BazelGlobalFunctions.isVarArgs(it),
                    BazelGlobalFunctions.isKwArgs(it)
                ));
            }
            Set<Environment> environment = new LinkedHashSet<>(function.environment());

            return new StarlarkFunctionInfo(params, environment);
        }

        private static boolean resolvesToValidTarget(StarlarkCallExpression receiver) {
            if (!(receiver.getCalledExpression() instanceof StarlarkReferenceExpression calledExpression)) {
                return false;
            }
            PsiReference reference = calledExpression.getReference();
            PsiElement resolved = reference == null ? null : reference.resolve();
            return resolved != null;
        }
    }

    // consulo: upstream top-level class of the same Kotlin file
    class StarlarkCallableFunctionInfoProvider implements StarlarkFunctionInfoProvider {
        @Override
        public @Nullable StarlarkFunctionInfo resolve(StarlarkCallExpression call) {
            StarlarkCallable callable = StarlarkCallableResolver.resolve(call);
            if (callable == null) {
                return null;
            }
            return new StarlarkFunctionInfo(getSignature(callable));
        }

        private List<StarlarkFuncParam> getSignature(StarlarkCallable callable) {
            StarlarkParameterList parameterList = PsiTreeUtil.getChildOfType(callable, StarlarkParameterList.class);
            List<PsiElement> signatureItems = new ArrayList<>();
            if (parameterList != null) {
                for (PsiElement it : parameterList.getChildren()) {
                    if (it instanceof StarlarkParameter || it instanceof StarlarkKeywordOnlyBoundary) {
                        signatureItems.add(it);
                    }
                }
            }

            boolean keywordOnly = false;
            List<StarlarkFuncParam> params = new ArrayList<>();

            for (PsiElement item : signatureItems) {
                if (item instanceof StarlarkKeywordOnlyBoundary) {
                    keywordOnly = true;
                }
                else if (item instanceof StarlarkVariadicParameter variadicParameter) {
                    String name = variadicParameter.getName();
                    if (name == null) {
                        continue;
                    }
                    params.add(new StarlarkFuncParam(
                        name,
                        /* positional = */ true,
                        /* named = */ false,
                        /* required = */ false,
                        /* isVarArgs = */ true,
                        /* isKwArgs = */ false
                    ));
                    keywordOnly = true;
                }
                else if (item instanceof StarlarkKeywordVariadicParameter keywordVariadicParameter) {
                    String name = keywordVariadicParameter.getName();
                    if (name == null) {
                        continue;
                    }
                    params.add(new StarlarkFuncParam(
                        name,
                        /* positional = */ false,
                        /* named = */ true,
                        /* required = */ false,
                        /* isVarArgs = */ false,
                        /* isKwArgs = */ true
                    ));
                }
                else if (item instanceof StarlarkMandatoryParameter mandatoryParameter) {
                    String name = mandatoryParameter.getName();
                    if (name == null) {
                        continue;
                    }
                    params.add(new StarlarkFuncParam(
                        name,
                        /* positional = */ !keywordOnly,
                        /* named = */ true,
                        /* required = */ true,
                        /* isVarArgs = */ false,
                        /* isKwArgs = */ false
                    ));
                }
                else if (item instanceof StarlarkOptionalParameter optionalParameter) {
                    String name = optionalParameter.getName();
                    if (name == null) {
                        continue;
                    }
                    params.add(new StarlarkFuncParam(
                        name,
                        /* positional = */ !keywordOnly,
                        /* named = */ true,
                        /* required = */ false,
                        /* isVarArgs = */ false,
                        /* isKwArgs = */ false
                    ));
                }
            }

            return params;
        }
    }
}
