package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.bazel.config.project.isBazelProject;
import static org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionLookupElement.getCompletionLookupElement;

public class StarlarkVisibilityReference extends PsiReferenceBase<StarlarkStringLiteralExpression> {
    @RequiredReadAction
    public StarlarkVisibilityReference(StarlarkStringLiteralExpression element) {
        super(element, new TextRange(0, element.getTextLength()), false);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        return null;
    }

    @Override
    public LookupElement[] getVariants() {
        Project project = myElement.getProject();
        if (!isBazelProject(project)) {
            return LookupElement.EMPTY_ARRAY;
        }
        Image icon = PlatformIconGroup.nodesVariable();
        List<String> shortTargets = TargetStorage.getTargetStorage(project).getAllTargetShortLabels();
        List<LookupElement> targetVisibilities = shortTargets.stream().map(it -> getCompletionLookupElement(it, icon)).toList();
        List<LookupElement> pkgVisibilities = shortTargets.stream().map(it -> getCompletionLookupElement(it + ":__pkg__", icon)).toList();
        List<LookupElement> subpkgVisibilities =
            shortTargets.stream().map(it -> getCompletionLookupElement(it + ":__subpackages__", icon)).toList();

        List<LookupElement> predefined =
            List.of(
                getCompletionLookupElement("//visibility:public", icon, 1.0),
                getCompletionLookupElement("//visibility:private", icon, 1.0)
            );
        // consulo: Kotlin array + array + array + list
        List<LookupElement> result = new ArrayList<>(pkgVisibilities);
        result.addAll(subpkgVisibilities);
        result.addAll(targetVisibilities);
        result.addAll(predefined);
        return result.toArray(LookupElement.EMPTY_ARRAY);
    }
}
