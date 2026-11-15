package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.cacheBuilder.DefaultWordsScanner;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets;
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkHighlightingLexer;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;

@ExtensionImpl
public class StarlarkFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(PsiElement psiElement) {
        return psiElement instanceof StarlarkNamedElement
            || psiElement instanceof StarlarkReferenceExpression
            || StarlarkCallExpression.isRuleTarget(psiElement);
    }

    @Override
    public String getHelpId(PsiElement psiElement) {
        if (psiElement instanceof StarlarkTargetExpression || psiElement instanceof StarlarkReferenceExpression) {
            return "reference.dialogs.findUsages.variable";
        }
        else {
            // consulo: HelpID.FIND_OTHER_USAGES (the HelpID class is in ide-impl)
            return "reference.dialogs.findUsages.other";
        }
    }

    @Override
    public String getType(PsiElement element) {
        if (StarlarkCallExpression.isRuleTarget(element)) {
            return "target";
        }
        else if (element instanceof StarlarkFunctionDeclaration) {
            return "function";
        }
        else if (element instanceof StarlarkTargetExpression || element instanceof StarlarkReferenceExpression) {
            return "variable";
        }
        else {
            return "";
        }
    }

    @Override
    public String getDescriptiveName(PsiElement element) {
        if (element instanceof StarlarkElement starlarkElement) {
            String name = starlarkElement.getName();
            return name != null ? name : "<anonymous>";
        }
        else {
            return element.toString();
        }
    }

    @Override
    public String getNodeText(PsiElement element, boolean useFullName) {
        return getDescriptiveName(element);
    }

    @Override
    public WordsScanner getWordsScanner() {
        return new StarlarkWordsScanner();
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}

class StarlarkWordsScanner extends DefaultWordsScanner {
    StarlarkWordsScanner() {
        super(
            new StarlarkHighlightingLexer(),
            StarlarkTokenSets.IDENTIFIER,
            StarlarkTokenSets.COMMENT,
            StarlarkTokenSets.STRINGS
        );
    }
}
