package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.content.ProjectFileIndex;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFilePresentation;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.Package;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.rename.StarlarkStringLiteralManipulator;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingProvider;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.jetbrains.bazel.commons.constants.Constants.BUILD_FILE_NAMES;
import static org.jetbrains.bazel.config.project.isBazelProject;
import static org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionLookupElement.getCompletionLookupElement;

// Tested in ExternalRepoResolveTest
public class BazelLabelReference extends PsiReferenceBase<StarlarkStringLiteralExpression> {
    @RequiredReadAction
    public BazelLabelReference(StarlarkStringLiteralExpression element, boolean soft) {
        super(element, new TextRange(0, element.getTextLength()), soft);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        Project project = myElement.getProject();
        if (!isBazelProject(project) || isInNameArgument()) {
            return null;
        }
        Label label = Label.parseOrNull(myElement.getStringContents());
        if (label == null) {
            return null;
        }
        boolean acceptOnlyFileTarget = PsiTreeUtil.getParentOfType(myElement, StarlarkLoadStatement.class, true) != null;
        return LabelResolveUtils.resolveLabel(
            project,
            label,
            myElement.getContainingFile().getOriginalFile().getVirtualFile(),
            acceptOnlyFileTarget
        );
    }

    @Override
    @RequiredReadAction
    public LookupElement[] getVariants() {
        if (!isBazelProject(myElement.getProject()) || isInNameArgument()) {
            return LookupElement.EMPTY_ARRAY;
        }

        if (isFileCompletionLocation()) {
            return fileCompletion();
        }
        if (isTargetCompletionLocation()) {
            return targetCompletion();
        }
        if (isLoadFilenameCompletionLocation()) {
            return loadFilenameCompletion();
        }
        if (isUseExtensionArgument()) {
            return loadFilenameCompletion();
        }

        return LookupElement.EMPTY_ARRAY;
    }

    // consulo: upstream returns null, Consulo declares a non-null return
    @Override
    @RequiredWriteAction
    @SuppressWarnings("NullAway")
    public @Nullable PsiElement bindToElement(PsiElement element) {
        VirtualFile file = element instanceof PsiFile psiFile ? psiFile.getVirtualFile() : null;
        if (file == null) {
            return null;
        }
        Path filePath = Path.of(file.getPath());
        VirtualFile repo = BazelRepoMappingUtils.findContainingBazelRepo(file);
        if (repo == null) {
            return null;
        }
        Path repoPath = Path.of(repo.getPath());
        VirtualFile buildFilePath = LabelResolveUtils.findBuildFilePathFor(file, repo);
        VirtualFile buildFileDir = buildFilePath == null ? null : buildFilePath.getParent();
        Path packagePath = buildFileDir == null ? null : Path.of(buildFileDir.getPath());
        if (packagePath == null) {
            return null;
        }

        String packageName = relativeToOrNull(packagePath, repoPath);
        if (packageName == null) {
            return null;
        }
        String targetName = relativeToOrNull(filePath, packagePath);
        if (targetName == null) {
            return null;
        }
        Map<String, Path> canonicalRepoNameToPath = BazelRepoMappingProvider.getCanonicalRepoNameToPath(element.getProject());
        // consulo: Kotlin Map.reverse()
        Map<Path, String> reversed = new HashMap<>();
        for (Map.Entry<String, Path> entry : canonicalRepoNameToPath.entrySet()) {
            reversed.put(entry.getValue(), entry.getKey());
        }
        String canonicalRepoName = reversed.get(repoPath);
        if (canonicalRepoName == null) {
            return null;
        }
        ResolvedLabel label =
            new ResolvedLabel(
                Canonical.createCanonicalOrMain(canonicalRepoName),
                new Package(List.of(packageName.split("/", -1))),
                new SingleTarget(targetName)
            );

        TextRange range = new StarlarkStringLiteralManipulator().getRangeInElement(this.myElement);
        String newContent = BazelRepoMappingUtils.toShortString(label, element.getProject());
        ElementManipulator<StarlarkStringLiteralExpression> manipulator = ElementManipulators.getManipulator(this.myElement);
        return manipulator == null ? null : manipulator.handleContentChange(this.myElement, range, newContent);
    }

    // consulo: Kotlin Path.relativeToOrNull(base)?.invariantSeparatorsPathString
    private static @Nullable String relativeToOrNull(Path receiver, Path base) {
        try {
            return base.relativize(receiver).toString().replace('\\', '/');
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String newElementName) {
        String newContent = buildNewLabelContent(myElement.getStringContents(), newElementName);
        TextRange range = ElementManipulators.getValueTextRange(myElement);
        ElementManipulator<StarlarkStringLiteralExpression> manipulator = ElementManipulators.getManipulator(myElement);
        StarlarkStringLiteralExpression result = manipulator == null ? null : manipulator.handleContentChange(myElement, range, newContent);
        return result != null ? result : myElement;
    }

    private String buildNewLabelContent(String oldContent, String newFilename) {
        Label label = Label.parseOrNull(oldContent);
        int fileNameIndex;
        if (label != null && oldContent.contains(":")) {
            fileNameIndex = oldContent.lastIndexOf(':') + 1 + label.target().toString().lastIndexOf('/');
        }
        else {
            fileNameIndex = oldContent.lastIndexOf('/');
        }
        return fileNameIndex >= 0 ? oldContent.substring(0, Math.min(fileNameIndex + 1, oldContent.length())) + newFilename : newFilename;
    }

    // Checks whether it is the value of "src", "srcs" or "hdrs".
    @RequiredReadAction
    private boolean isFileCompletionLocation() {
        if (myElement.getParent() instanceof StarlarkListLiteralExpression) {
            String parentName = myElement.getParent().getParent() instanceof StarlarkNamedArgumentExpression namedArgument
                ? namedArgument.getName()
                : null;
            // consulo: List.of(...).contains(null) throws, Kotlin `in` returns false
            return parentName != null && List.of("srcs", "hdrs").contains(parentName);
        }
        return myElement.getParent() instanceof StarlarkNamedArgumentExpression namedArgument
            && Objects.equals(namedArgument.getName(), "src");
    }

    // Returns an array of all files (excluding BUILD ones) from the current directory and its
    // subdirectories, excluding subpackages — that is, subdirectories that contain a BUILD file.
    @RequiredReadAction
    private LookupElement[] fileCompletion() {
        VirtualFile currentDirectory =
            Objects.requireNonNull(Objects.requireNonNull(myElement.getContainingFile().getOriginalFile().getVirtualFile()).getParent());
        List<VirtualFile> allFiles = new ArrayList<>();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(myElement.getProject());
        searchForAllFiles(projectFileIndex, currentDirectory, allFiles);

        // `VfsUtilCore.getRelativePath` can return null in the following two cases:
        // 1. The two arguments (file and ancestor) belong to different file systems.
        // 2. The second argument is not an ancestor of the first one.
        // However, since the first argument is created by traversing down the directory
        // tree starting from the second one, neither of these conditions should occur.
        List<LookupElement> lookupElements = new ArrayList<>();
        for (VirtualFile it : allFiles) {
            String elementName = VirtualFileUtil.getRelativePath(it, currentDirectory);
            if (elementName == null) {
                continue;
            }
            // consulo: IconUtil.getIcon(file, 0, project) -> VirtualFileManager.getFileIcon (nullable)
            Image icon = VirtualFileManager.getInstance().getFileIcon(it, myElement.getProject(), 0);
            lookupElements.add(getCompletionLookupElement(elementName, icon != null ? icon : VirtualFilePresentation.getIcon(it)));
        }
        return lookupElements.toArray(LookupElement.EMPTY_ARRAY);
    }

    private void searchForAllFiles(
        ProjectFileIndex index,
        VirtualFile currentDirectory,
        List<VirtualFile> allFiles
    ) {
        if (index.isExcluded(currentDirectory)) {
            return;
        }
        VirtualFile[] children = Objects.requireNonNull(currentDirectory.getChildren());
        for (VirtualFile child : children) {
            // consulo: Kotlin VirtualFile.isFile
            if (child.isValid() && !child.isDirectory() && !isBazelFile(child)) {
                allFiles.add(child);
            }
            else if (child.isDirectory()) {
                if (LabelResolveUtils.findBuildFile(child) == null) {
                    searchForAllFiles(index, child, allFiles);
                }
            }
        }
    }

    private static boolean isBazelFile(VirtualFile receiver) {
        return Arrays.stream(BUILD_FILE_NAMES).anyMatch(it -> receiver.getName().equals(it));
    }

    @RequiredReadAction
    private boolean isTargetCompletionLocation() { // TODO: Correct target completion location validation.
        PsiElement parent = myElement.getParent();
        if (parent == null) {
            return false;
        }
        return parent instanceof StarlarkListLiteralExpression;
    }

    private LookupElement[] targetCompletion() {
        Project project = myElement.getProject();
        return TargetStorage.getTargetStorage(project)
            .getAllTargetShortLabels()
            .stream()
            .map(it -> getCompletionLookupElement(it, PlatformIconGroup.nodesPackage()))
            .toArray(LookupElement[]::new);
    }

    @RequiredReadAction
    private boolean isInNameArgument() {
        PsiElement parent = myElement.getParent();
        if (parent == null) {
            return false;
        }
        return parent instanceof StarlarkNamedArgumentExpression namedArgument && namedArgument.isNameArgument();
    }

    @RequiredReadAction
    private boolean isLoadFilenameCompletionLocation() {
        return myElement.getParent() instanceof StarlarkFilenameLoadValue;
    }

    private LookupElement[] loadFilenameCompletion() {
        Map<String, List<Label>> repoNameToBzlFiles = BazelBzlFileService.getCanonicalRepoNameToBzlFiles(myElement.getProject());
        List<LookupElement> lookupElements = new ArrayList<>();
        for (List<Label> bzlFiles : repoNameToBzlFiles.values()) {
            for (Label label : bzlFiles) {
                lookupElements.add(
                    getCompletionLookupElement(
                        BazelRepoMappingUtils.toShortString(label, myElement.getProject()),
                        PlatformIconGroup.filetypesAny_type()
                    )
                );
            }
        }
        return lookupElements.toArray(LookupElement.EMPTY_ARRAY);
    }

    @RequiredReadAction
    private boolean isUseExtensionArgument() {
        return myElement.getParent() instanceof StarlarkArgumentExpression argumentExpression
            && argumentExpression.getParent() instanceof StarlarkArgumentList argumentList
            && argumentList.getParent() instanceof StarlarkCallExpression callExpression
            && Objects.equals(callExpression.getCalledFunctionName(), "use_extension");
    }
}
