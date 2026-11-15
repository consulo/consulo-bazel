// consulo: package org.jetbrains.bazel.languages.starlark.references is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.languages.starlark.references.java;

import com.intellij.java.language.psi.JavaPsiFacade;
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jspecify.annotations.Nullable;

public class StarlarkClassnameReference extends PsiReferenceBase<StarlarkStringLiteralExpression> {
    public StarlarkClassnameReference(StarlarkStringLiteralExpression element) {
        super(element, new TextRange(0, element.getTextLength()), false);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        Project project = getElement().getProject();
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }

        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        // JavaPsiFacade works here both for Java and Kotlin. It is tested in
        // org.jetbrains.bazel.languages.starlark.references.StarlarkClassnameReferenceTest.
        return JavaPsiFacade.getInstance(project).findClass(getElement().getStringContents(), scope);
    }
}
