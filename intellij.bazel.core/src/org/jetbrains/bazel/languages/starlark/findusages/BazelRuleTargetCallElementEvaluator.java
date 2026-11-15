package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;

/**
 * It makes {@link StarlarkCallExpression} being {@link consulo.language.psi.PsiNameIdentifierOwner} effective only for target rule
 * declarations.
 * Without this every call expression would be considered as "named" and it would allow all the find usages and refactoring actions.
 */
// TODO consulo: TargetElementEvaluatorEx2 (language extension "targetElementEvaluator") is missing. Consulo's
//  TargetElementUtil.getNamedElement accepts the PsiNamedElement parent without an isAcceptableNamedParent hook and
//  TargetElementUtilExtender has no equivalent, so this class is not registered.
public class BazelRuleTargetCallElementEvaluator {
    public boolean isAcceptableNamedParent(PsiElement parent) {
        return !(parent instanceof StarlarkCallExpression callExpression) || callExpression.isRuleTarget();
    }
}
