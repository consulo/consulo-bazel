package org.jetbrains.bazel.languages.starlark.rename;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.rename.RenameInputValidator;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import org.jetbrains.bazel.label.LabelValidator;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class BazelTargetRenameInputValidator implements RenameInputValidator {
    private final ElementPattern<StarlarkCallExpression> myPattern = PlatformPatterns
        .psiElement(StarlarkCallExpression.class)
        .with(BazelTargetRuleDeclarationPatternCondition.INSTANCE);

    @Override
    public ElementPattern<? extends PsiElement> getPattern() {
        return myPattern;
    }

    @Override
    public boolean isInputValid(
        String newName,
        PsiElement element,
        ProcessingContext context
    ) {
        return LabelValidator.isTargetNameValid(newName);
    }
}

class BazelTargetRuleDeclarationPatternCondition extends PatternCondition<StarlarkCallExpression> {
    static final BazelTargetRuleDeclarationPatternCondition INSTANCE = new BazelTargetRuleDeclarationPatternCondition();

    private BazelTargetRuleDeclarationPatternCondition() {
        super("isTargetRuleDeclaration");
    }

    @Override
    public boolean accepts(
        StarlarkCallExpression element,
        @Nullable ProcessingContext context
    ) {
        return element.isRuleTarget();
    }
}
