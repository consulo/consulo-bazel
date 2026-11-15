package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AmbiguousEmptyTarget;
import org.jetbrains.bazel.label.Label.Main;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingProvider;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class LabelResolveUtils {
    private LabelResolveUtils() {
    }

    // consulo: overload for the upstream default arguments (containingFile = null, acceptOnlyFileTarget = false)
    @RequiredReadAction
    public static @Nullable PsiElement resolveLabel(Project project, Label label) {
        return resolveLabel(project, label, null, false);
    }

    // consulo: overload for the upstream default argument acceptOnlyFileTarget = false
    @RequiredReadAction
    public static @Nullable PsiElement resolveLabel(Project project, Label label, @Nullable VirtualFile containingFile) {
        return resolveLabel(project, label, containingFile, false);
    }

    /**
     * @param containingFile       the file that should be used as context for resolving, e.g., relative labels
     * @param acceptOnlyFileTarget `true` if {@link Label#target()} always refers to a file on disk (e.g., in `load` statements).
     * @see "ExternalRepoResolveTest"
     */
    // upstream: @RequiresReadLock(generateAssertion = false /* IJPL-115548 */)
    @RequiredReadAction
    public static @Nullable PsiElement resolveLabel(
        Project project,
        Label label,
        @Nullable VirtualFile containingFile,
        boolean acceptOnlyFileTarget
    ) {
        BuildFileOrSourceFile buildOrSource = resolveBuildFileOrSourceFile(project, label, containingFile);
        if (buildOrSource == null) {
            return null;
        }
        if (buildOrSource instanceof BuildFilePsi buildFilePsi) {
            StarlarkFile buildFile = buildFilePsi.getFile();
            if (!acceptOnlyFileTarget) {
                PsiElement buildFileTarget = resolveBuildFileTarget(buildFile, label);
                if (buildFileTarget != null) {
                    return buildFileTarget;
                }
            }
            PsiFile fileTarget = resolveFileTarget(project, buildFile, label);
            if (fileTarget != null) {
                return fileTarget;
            }
            // Fall back to the BUILD file, as opposed to a specific target inside it.
            // The reference may still be valid, e.g., if a macro in the file creates a target with a custom name,
            // in which case we can't determine which macro actually corresponds to the target with that name.
            return !acceptOnlyFileTarget ? buildFile : null;
        }
        else {
            return PsiManager.getInstance(project).findFile(((SourceFile) buildOrSource).getFile());
        }
    }

    // consulo: overload for the upstream default argument containingFile = null
    public static @Nullable VirtualFile resolveFileTargetToVirtualFile(Project project, Label label) {
        return resolveFileTargetToVirtualFile(project, label, null);
    }

    /**
     * Resolves a single Bazel file target {@code label} to the {@link VirtualFile} it points to.
     * It only considers regular files. If label points to target or directory, it returns null.
     * <p>
     * Unlike {@link #resolveLabel}, this method works entirely at the VFS level and never touches PSI.
     */
    public static @Nullable VirtualFile resolveFileTargetToVirtualFile(
        Project project,
        Label label,
        @Nullable VirtualFile containingFile
    ) {
        if (label instanceof ResolvedLabel resolvedLabel) {
            VirtualFile packageDir = findReferredPackage(project, resolvedLabel, containingFile);
            VirtualFile file = packageDir == null ? null : packageDir.findFileByRelativePath(resolvedLabel.getTargetName());
            return file != null && isFile(file) ? file : null;
        }
        else {
            VirtualFile containingPackage = findContainingPackage(containingFile);
            VirtualFile referred =
                containingPackage == null ? null : containingPackage.findFileByRelativePath(label.packagePath().toString());
            if (referred != null && isFile(referred) && label.target() instanceof AmbiguousEmptyTarget) {
                return referred;
            }
            else {
                VirtualFile file = referred == null ? null : referred.findFileByRelativePath(label.getTargetName());
                return file != null && isFile(file) ? file : null;
            }
        }
    }

    // consulo: Kotlin VirtualFile.isFile
    private static boolean isFile(VirtualFile receiver) {
        return receiver.isValid() && !receiver.isDirectory();
    }

    private sealed interface BuildFileOrSourceFile permits BuildFilePsi, SourceFile {
    }

    private static final class BuildFilePsi implements BuildFileOrSourceFile {
        private final StarlarkFile myFile;

        BuildFilePsi(StarlarkFile file) {
            myFile = file;
        }

        StarlarkFile getFile() {
            return myFile;
        }
    }

    private static final class SourceFile implements BuildFileOrSourceFile {
        private final VirtualFile myFile;

        SourceFile(VirtualFile file) {
            myFile = file;
        }

        VirtualFile getFile() {
            return myFile;
        }
    }

    @RequiredReadAction
    private static @Nullable BuildFileOrSourceFile resolveBuildFileOrSourceFile(
        Project project,
        Label label,
        @Nullable VirtualFile containingFile
    ) {
        if (label instanceof ResolvedLabel resolvedLabel) {
            StarlarkFile buildFile = findBuildFile(
                project,
                resolvedLabel,
                containingFile
            );
            return buildFile != null ? new BuildFilePsi(buildFile) : null;
        }
        else {
            return resolveRelativePackageOrSourceFile(project, label, containingFile);
        }
    }

    @RequiredReadAction
    private static @Nullable PsiElement resolveBuildFileTarget(StarlarkFile buildFile, Label label) {
        SingleTarget target = BazelRepoMappingUtils.singleTarget(label);
        if (target == null) {
            return null;
        }
        return buildFile.findTargetRule(target.targetName());
    }

    // consulo: overload for the upstream default argument containingFile = null
    @RequiredReadAction
    public static @Nullable StarlarkFile findBuildFile(Project project, ResolvedLabel label) {
        return findBuildFile(project, label, null);
    }

    @RequiredReadAction
    public static @Nullable StarlarkFile findBuildFile(
        Project project,
        ResolvedLabel label,
        @Nullable VirtualFile containingFile
    ) {
        VirtualFile packageDir = findReferredPackage(project, label, containingFile);
        if (packageDir == null) {
            return null;
        }
        return findBuildFilePsi(project, packageDir);
    }

    @RequiredReadAction
    private static @Nullable BuildFileOrSourceFile resolveRelativePackageOrSourceFile(
        Project project,
        Label label,
        @Nullable VirtualFile containingFile
    ) {
        VirtualFile containingPackage = findContainingPackage(containingFile);
        if (containingPackage == null) {
            return null;
        }
        VirtualFile referredPackage = containingPackage.findFileByRelativePath(label.packagePath().toString());
        if (referredPackage == null) {
            return null;
        }
        if (isFile(referredPackage)) {
            if (label.target() instanceof AmbiguousEmptyTarget) {
                return new SourceFile(referredPackage);
            }
            return null;
        }
        StarlarkFile buildFilePsi = findBuildFilePsi(project, referredPackage);
        return buildFilePsi != null ? new BuildFilePsi(buildFilePsi) : null;
    }

    private static @Nullable VirtualFile findContainingPackage(@Nullable VirtualFile directory) {
        if (directory == null) {
            return null;
        }
        return findBuildFile(directory) != null ? directory : findContainingPackage(directory.getParent());
    }

    @RequiredReadAction
    private static @Nullable PsiFile resolveFileTarget(
        Project project,
        StarlarkFile buildFilePsi,
        Label label
    ) {
        // buildFilePsi may be excluded, in which case we can't get the parent PsiDirectory.
        // Therefore get virtualFile then parent and not vice-versa
        VirtualFile virtualFile = buildFilePsi.getVirtualFile();
        VirtualFile parent = virtualFile == null ? null : virtualFile.getParent();
        VirtualFile targetFile = parent == null ? null : parent.findFileByRelativePath(label.getTargetName());
        if (targetFile == null) {
            return null;
        }

        return PsiManager.getInstance(project).findFile(targetFile);
    }

    // consulo: overload for the upstream default argument containingFile = null
    public static @Nullable VirtualFile findReferredPackage(Project project, ResolvedLabel label) {
        return findReferredPackage(project, label, null);
    }

    // BAZEL-2280 LabelResolveUtilsKt#findReferredAbsolutePackage may not be able to resolve repo root
    public static @Nullable VirtualFile findReferredPackage(
        Project project,
        ResolvedLabel label,
        @Nullable VirtualFile containingFile
    ) {
        ResolvedLabel canonicalLabel = BazelRepoMappingUtils.toCanonicalLabel(label, project);
        if (canonicalLabel == null) {
            return null;
        }
        VirtualFile repoRoot;
        if (canonicalLabel.repo() instanceof Main && containingFile != null) {
            repoRoot = BazelRepoMappingUtils.findContainingBazelRepo(containingFile);
        }
        else {
            Path repoPath = BazelRepoMappingProvider.getCanonicalRepoNameToPath(project).get(canonicalLabel.getRepoName());
            repoRoot = repoPath == null ? null : PathUtil.findVirtualFile(repoPath);
        }
        if (repoRoot == null) {
            return null;
        }

        return repoRoot.findFileByRelativePath(
            String.join(String.valueOf(VirtualFileUtil.VFS_SEPARATOR_CHAR), label.packagePath().pathSegments())
        );
    }

    public static @Nullable VirtualFile findBuildFile(VirtualFile packageDir) {
        for (String buildFileName : Constants.BUILD_FILE_NAMES) {
            VirtualFile child = packageDir.findChild(buildFileName);
            if (child != null && isFile(child)) {
                return child;
            }
        }
        return null;
    }

    @RequiredReadAction
    private static @Nullable StarlarkFile findBuildFilePsi(Project project, VirtualFile packageDir) {
        VirtualFile buildFile = findBuildFile(packageDir);
        if (buildFile == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(buildFile) instanceof StarlarkFile starlarkFile ? starlarkFile : null;
    }

    public static @Nullable VirtualFile findBuildFilePathFor(
        VirtualFile file,
        VirtualFile repoRoot
    ) {
        if (!VirtualFileUtil.isAncestor(repoRoot, file, false)) {
            return null;
        }

        VirtualFile dir;
        if (file.isDirectory()) {
            dir = file;
        }
        else {
            dir = file.getParent();
            if (dir == null) {
                return null;
            }
        }
        // consulo: Kotlin generateSequence(dir) { it.takeIf { it != repoRoot }?.parent }.firstNotNullOfOrNull { findBuildFile(it) }
        for (VirtualFile it = dir; it != null; it = !it.equals(repoRoot) ? it.getParent() : null) {
            VirtualFile buildFile = findBuildFile(it);
            if (buildFile != null) {
                return buildFile;
            }
        }
        return null;
    }
}
