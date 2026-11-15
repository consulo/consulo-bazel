package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.DocumentationLinkHandler;
import consulo.bazel.documentation.DocumentationTarget;
import consulo.bazel.documentation.LinkResolveResult;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class BazelRuleCallDocumentationLinkHandler implements DocumentationLinkHandler {
    @Override
    public @Nullable LinkResolveResult resolveLink(DocumentationTarget target, String url) {
        if (!(target instanceof BazelRuleCallDocumentationTarget bazelTargetDoc)) {
            return null;
        }
        Label label = StarlarkDocumentationLinks.labelFrom(url);
        if (label == null) {
            return null;
        }
        PsiFile psiFile = bazelTargetDoc.getTargetExpression().getContainingFile();
        VirtualFile containingFile = psiFile == null ? null : psiFile.getVirtualFile();
        if (containingFile == null) {
            return null;
        }
        // consulo: upstream default argument acceptOnlyFileTarget = false
        PsiElement resolved =
            LabelResolveUtils.resolveLabel(bazelTargetDoc.getTargetExpression().getProject(), label, containingFile, false);
        if (resolved == null) {
            return null;
        }
        if (!(resolved instanceof StarlarkCallExpression resolvedCall)) {
            return null;
        }
        return LinkResolveResult.resolvedTarget(new BazelRuleCallDocumentationTarget(resolvedCall));
    }
}
