package org.jetbrains.bazel.languages.projectview;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.workspace.BazelProjectDirectoriesUtils;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

@ServiceImpl
@Singleton
public class TargetUtilsProjectViewCodeInsightSupport implements ProjectViewCodeInsightSupport {
    private final Project myProject;

    @Inject
    public TargetUtilsProjectViewCodeInsightSupport(Project project) {
        myProject = project;
    }

    @Override
    public Stream<String> getAllTargetLabels() {
        return TargetStorage.getTargetStorage(myProject).getAllTargetShortLabels().stream();
    }

    @Override
    public Set<VirtualFile> getExcludedRoots() {
        Set<VirtualFile> excludedRoots = BazelProjectDirectoriesUtils.excludedRoots(myProject);
        return excludedRoots != null ? excludedRoots : Set.of();
    }

    @Override
    public @Nullable PsiElement resolvePsiFromLabel(
        Label label,
        @Nullable VirtualFile containingFile,
        boolean acceptOnlyFileTarget
    ) {
        return LabelResolveUtils.resolveLabel(myProject, label, containingFile, acceptOnlyFileTarget);
    }
}
