package org.jetbrains.bazel.languages.starlark.globbing;

import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class StarlarkGlobExpressionGlob {
    private StarlarkGlobExpressionGlob() {
    }

    private static @Nullable StarlarkGlob buildGlob(StarlarkGlobExpression receiver) {
        PsiFile containingFile = receiver.getContainingFile();
        PsiDirectory parent = containingFile == null ? null : containingFile.getParent();
        VirtualFile containingDirectory = parent == null ? null : parent.getVirtualFile();
        if (containingDirectory == null) {
            return null;
        }

        return StarlarkGlob
            .forPath(containingDirectory)
            .addPatterns(listContents(receiver.getIncludes()))
            .addExcludes(listContents(receiver.getExcludes()))
            .setExcludeDirectories(receiver.areDirectoriesExcluded())
            .setDirectoryFilter(directoryFilter(containingDirectory.getPath()))
            .build();
    }

    // consulo: local function of buildGlob()
    private static List<String> listContents(@Nullable PsiElement expr) {
        if (!(expr instanceof StarlarkListLiteralExpression listLiteralExpression)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (PsiElement element : listLiteralExpression.getElements()) {
            if (element instanceof StarlarkStringLiteralExpression stringLiteralExpression) {
                result.add(stringLiteralExpression.getStringContents());
            }
        }
        return result;
    }

    // consulo: local function of buildGlob()
    private static Predicate<VirtualFile> directoryFilter(String base) {
        return dir -> dir.getPath().equals(base) || LabelResolveUtils.findBuildFile(dir) == null;
    }

    public static @Nullable StarlarkGlob getGlob(StarlarkGlobExpression receiver) {
        return LanguageCachedValueUtil.getCachedValue(
            receiver,
            () -> CachedValueProvider.Result.create(buildGlob(receiver), PsiModificationTracker.MODIFICATION_COUNT)
        );
    }
}
