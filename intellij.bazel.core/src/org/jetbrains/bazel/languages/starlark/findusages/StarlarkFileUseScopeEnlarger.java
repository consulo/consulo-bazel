package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.UseScopeEnlarger;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jspecify.annotations.Nullable;

import static org.jetbrains.bazel.config.project.isBazelProject;

/**
 * Enlarges the search scope for non-starlark files e.g. Kotlin/Java sources. It provides correct search scope for
 * {@link StarlarkFileUsageSearcher}.
 * <p>
 * By default, non-starlark files are private to the package they belong to.
 * However, they can be exported with <a href="https://bazel.build/reference/be/functions#exports_files">export_files</a> or
 * the default visibility of the package might be changed to e.g. public, making those files available in other packages.
 * So in practice, any non-starlark file might potentially be referred from any package. That's why
 * {@link StarlarkSearchScope#starlarkProjectScope} is used here.
 **/
@ExtensionImpl
public class StarlarkFileUseScopeEnlarger implements UseScopeEnlarger {
    @Override
    public @Nullable SearchScope getAdditionalUseScope(PsiElement element) {
        if (!isBazelProject(element.getProject()) || !(element instanceof PsiFile) || element instanceof StarlarkElement) {
            return null;
        }
        else {
            return StarlarkSearchScope.starlarkProjectScope(element.getProject());
        }
    }
}
