package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlobExpressionGlob;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StarlarkGlobReference extends PsiPolyVariantReferenceBase<StarlarkGlobExpression> implements PsiPolyVariantReference {
    @RequiredReadAction
    public StarlarkGlobReference(StarlarkGlobExpression element) {
        super(
            element,
            new TextRange(0, element.getTextLength())
        );
    }

    @Override
    @RequiredReadAction
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Project project = myElement.getProject();

        try {
            StarlarkGlob glob = StarlarkGlobExpressionGlob.getGlob(myElement);
            List<VirtualFile> files = glob != null ? glob.execute() : List.of();

            List<ResolveResult> results = new ArrayList<>();
            for (VirtualFile file : files) {
                PsiFileSystemItem psiFile = resolveFile(file, project);
                if (psiFile != null) {
                    results.add(new PsiElementResolveResult(psiFile));
                }
            }

            return results.toArray(ResolveResult.EMPTY_ARRAY);
        }
        catch (Exception ignored) {
            return ResolveResult.EMPTY_ARRAY;
        }
    }

    @RequiredReadAction
    public @Nullable PsiFileSystemItem resolveFile(VirtualFile vf, Project project) {
        PsiManager manager = PsiManager.getInstance(project);
        return vf.isDirectory() ? manager.findDirectory(vf) : manager.findFile(vf);
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String newElementName) {
        return myElement;
    }

    @Override
    @RequiredWriteAction
    public PsiElement bindToElement(PsiElement element) {
        return myElement;
    }
}
