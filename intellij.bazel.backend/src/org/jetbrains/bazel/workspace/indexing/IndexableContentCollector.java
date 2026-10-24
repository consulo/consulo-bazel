package org.jetbrains.bazel.workspace.indexing;

import consulo.application.Application;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.workspace.ProjectViewGlobSet;
import org.jetbrains.bazel.workspace.indexing.IndexableContent.MutableIndexableContent;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Selects the files that should be indexed but are not under {@code contentRoots} already.
 * Files to index:
 * 1. All files matched by `index` patterns and the {@code BAZEL_FILES_PATTERNS} patterns in {@code includedRoots}, where the
 * inner-most root wins.
 * 2. The workspace files directly in the project root directory
 * 3. The selected project view file
 * 4. The files from {@link IndexableContentContributor}.
 */
public class IndexableContentCollector {
    private static final List<String> BAZEL_FILES_PATTERNS;

    static {
        List<String> patterns = new ArrayList<>();
        patterns.addAll(Arrays.asList(Constants.WORKSPACE_FILE_NAMES));
        patterns.addAll(Arrays.asList(Constants.BUILD_FILE_NAMES));
        patterns.add(Constants.MODULE_BAZEL_FILE_NAME);
        for (String extension : Constants.SUPPORTED_EXTENSIONS) {
            patterns.add("*." + extension);
        }
        BAZEL_FILES_PATTERNS = List.copyOf(patterns);
    }

    private final Project myProject;
    private final Set<VirtualFile> myIncludedRoots;
    private final Set<VirtualFile> myExcludedRoots;
    private final Set<VirtualFile> myContentRoots;

    private final VirtualFile myProjectRoot;
    private final List<String> myWorkspaceFileNames = Arrays.asList(Constants.WORKSPACE_FILE_NAMES);
    private final @Nullable Path myProjectViewPath;
    private final ProjectViewGlobSet myIndexingGlob;

    public IndexableContentCollector(
        Project project,
        List<String> indexPatterns,
        Set<VirtualFile> includedRoots,
        Set<VirtualFile> excludedRoots,
        Set<VirtualFile> contentRoots
    ) {
        myProject = project;
        myIncludedRoots = includedRoots;
        myExcludedRoots = excludedRoots;
        myContentRoots = contentRoots;
        myProjectRoot = org.jetbrains.bazel.config.project.getRootDir(project);
        myProjectViewPath = ProjectViewService.projectViewPath(project);
        List<String> patterns = new ArrayList<>(indexPatterns);
        patterns.addAll(BAZEL_FILES_PATTERNS);
        myIndexingGlob = ProjectViewGlobSet.of(myProjectRoot.toNioPath(), patterns);
    }

    // consulo: no VirtualFileUrlManager, roots are VFS urls
    public IndexableContent collect() {
        return IndexableContent.buildIndexableContent(content -> {
            addIncludedIndexableContent(content);
            addAlwaysIndexableContent(content);
        });
    }

    /**
     * Returns true if a given file should be indexed.
     */
    public boolean shouldBeIndexed(VirtualFile file) {
        return file.isValid() &&
            !file.isDirectory() &&
            (isRootWorkspaceFile(file) || isProjectViewFile(file) || isMatchUnderIncludedRoots(file));
    }

    private void addIncludedIndexableContent(MutableIndexableContent content) {
        Set<VirtualFile> visited = new HashSet<>();
        for (VirtualFile includedRoot : myIncludedRoots) {
            if (!isUnderIndexableIncludedRoot(includedRoot)) {
                continue;
            }
            VirtualFileUtil.visitChildrenRecursively(
                includedRoot,
                new VirtualFileVisitor<Void>() {
                    @Override
                    public Result visitFileEx(VirtualFile file) {
                        if (myExcludedRoots.contains(file) || myContentRoots.contains(file) || !visited.add(file)) {
                            return SKIP_CHILDREN;
                        }
                        else if (file.isDirectory() && impliesRecursiveMatch(file)) {
                            content.getRecursiveRoots().add(file.getUrl());
                            return SKIP_CHILDREN;
                        }
                        else if (!file.isDirectory() && matchesIndexingGlob(file)) {
                            content.getNonRecursiveRoots().add(file.getUrl());
                            return CONTINUE;
                        }
                        else {
                            return CONTINUE;
                        }
                    }
                }
            );
        }
    }

    private void addAlwaysIndexableContent(MutableIndexableContent content) {
        for (String workspaceFileName : myWorkspaceFileNames) {
            VirtualFile file = myProjectRoot.findChild(workspaceFileName);
            if (file != null) {
                content.getNonRecursiveRoots().add(file.getUrl());
            }
        }
        if (myProjectViewPath != null) {
            content.getNonRecursiveRoots().add(VirtualFileUtil.pathToUrl(myProjectViewPath.toString()));
        }
        Application.get().getExtensionPoint(IndexableContentContributor.class).forEachExtensionSafe(contributor ->
            content.addAll(contributor.getIndexableContent(myProject))
        );
    }

    private boolean isRootWorkspaceFile(VirtualFile file) {
        return Objects.equals(file.getParent(), myProjectRoot) && myWorkspaceFileNames.contains(file.getName());
    }

    private boolean isProjectViewFile(VirtualFile file) {
        if (myProjectViewPath == null) {
            return false;
        }
        Path path = toNioPathOrNull(file);
        return myProjectViewPath.equals(path);
    }

    private boolean isMatchUnderIncludedRoots(VirtualFile file) {
        return isUnderIndexableIncludedRoot(file) && matchesIndexingGlob(file);
    }

    /**
     * Returns true if the inner-most root above the file is an included root, and no content root contains that included root.
     * An excluded root above the included root does not change the result.
     * The file itself counts as a root, so this also selects the included roots that {@link #addIncludedIndexableContent} walks.
     */
    private boolean isUnderIndexableIncludedRoot(VirtualFile file) {
        boolean isUnderIncludedRoot = false;
        VirtualFile current = file;
        while (current != null) {
            if (myContentRoots.contains(current)) {
                return false;
            }
            else if (myExcludedRoots.contains(current)) {
                return isUnderIncludedRoot;
            }
            else if (myIncludedRoots.contains(current)) {
                isUnderIncludedRoot = true;
            }
            current = current.getParent();
        }
        return isUnderIncludedRoot;
    }

    private boolean matchesIndexingGlob(VirtualFile file) {
        Path path = toNioPathOrNull(file);
        return path != null && myIndexingGlob.matches(path);
    }

    private boolean impliesRecursiveMatch(VirtualFile file) {
        Path path = toNioPathOrNull(file);
        return path != null && myIndexingGlob.impliesRecursiveMatch(path);
    }

    // consulo: Kotlin VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
