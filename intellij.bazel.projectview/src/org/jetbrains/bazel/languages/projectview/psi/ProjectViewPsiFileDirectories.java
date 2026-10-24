package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static org.jetbrains.bazel.config.project.getRootDir;

// consulo: upstream Kotlin top-level extension functions of ProjectViewPsiFile
public final class ProjectViewPsiFileDirectories {
    private ProjectViewPsiFileDirectories() {
    }

    public static boolean isDirectoriesNullOrEmpty(ProjectViewPsiFile receiver) {
        ProjectViewPsiSection directories = getDirectories(receiver);
        return directories == null || directories.getItems().isEmpty();
    }

    public static boolean directoriesContainsInclude(ProjectViewPsiFile receiver, VirtualFile file) {
        return directoriesContainsItem(receiver, file, true);
    }

    public static boolean directoriesContainsExclude(ProjectViewPsiFile receiver, VirtualFile file) {
        return directoriesContainsItem(receiver, file, false);
    }

    public static void addDirectoriesInclude(ProjectViewPsiFile receiver, VirtualFile file) {
        addDirectoriesItem(receiver, file, true);
    }

    public static void addDirectoriesExclude(ProjectViewPsiFile receiver, VirtualFile file) {
        addDirectoriesItem(receiver, file, false);
    }

    public static void removeDirectoriesInclude(ProjectViewPsiFile receiver, VirtualFile file) {
        removeDirectoriesItem(receiver, file, true);
    }

    public static void removeDirectoriesExclude(ProjectViewPsiFile receiver, VirtualFile file) {
        removeDirectoriesItem(receiver, file, false);
    }

    private static boolean directoriesContainsItem(ProjectViewPsiFile receiver, VirtualFile file, boolean include) {
        Project project = receiver.getProject();
        ProjectViewPsiSection directories = getDirectories(receiver);
        String pathString = rootRelativePath(project, file);
        if (pathString == null) {
            return false;
        }
        String item = include ? pathString : "-" + pathString;
        return directories != null && findItemByText(directories, item) != null;
    }

    private static void addDirectoriesItem(ProjectViewPsiFile receiver, VirtualFile file, boolean include) {
        Project project = receiver.getProject();
        ProjectViewPsiSection directories = getDirectories(receiver);
        String pathString = rootRelativePath(project, file);
        if (pathString == null) {
            return;
        }
        String item = include ? pathString : "-" + pathString;
        if (directories == null) {
            ProjectViewPsiSection newDirectorySection = createDirectoriesSection(project, item);
            if (newDirectorySection == null) {
                return;
            }
            receiver.addBefore(newDirectorySection, receiver.getFirstChild());
            return;
        }
        ProjectViewPsiSection newDirectorySection = createDirectoriesSection(project, item);
        if (newDirectorySection == null) {
            return;
        }
        PsiElement colonPsi = newDirectorySection.getColon();
        if (colonPsi == null) {
            return;
        }
        List<ProjectViewPsiSectionItem> directoriesItems = directories.getItems();
        directories.addRangeAfter(
            Objects.requireNonNull(colonPsi.getNextSibling()),
            newDirectorySection.getItems().get(0),
            Objects.requireNonNull(
                directoriesItems.isEmpty() ? directories.getColon() : directoriesItems.get(directoriesItems.size() - 1)
            )
        );
    }

    private static void removeDirectoriesItem(ProjectViewPsiFile receiver, VirtualFile file, boolean include) {
        Project project = receiver.getProject();
        ProjectViewPsiSection directories = getDirectories(receiver);
        if (directories == null) {
            return;
        }
        String pathString = rootRelativePath(project, file);
        if (pathString == null) {
            return;
        }
        String itemText = include ? pathString : "-" + pathString;
        List<ProjectViewPsiSectionItem> items = directories.getItems();
        items.forEach(item -> {
            if (item.getText().equals(itemText)) {
                PsiElement prevSibling = item.getPrevSibling();
                PsiElement from = prevSibling != null && PsiTreeUtil.isAncestor(directories, prevSibling, false)
                    ? prevSibling
                    : directories.getColon();
                PsiElement nextSibling = item.getNextSibling();
                PsiElement to = nextSibling != null && PsiTreeUtil.isAncestor(directories, nextSibling, false)
                    ? nextSibling
                    : directories.getLastChild();
                directories.deleteChildRange(Objects.requireNonNull(from), Objects.requireNonNull(to));
            }
        });
    }

    private static @Nullable ProjectViewPsiSection getDirectories(ProjectViewPsiFile receiver) {
        return receiver.getSection("directories");
    }

    private static @Nullable String rootRelativePath(Project receiver, VirtualFile file) {
        String relativePath = VirtualFileUtil.getRelativePath(file, getRootDir(receiver));
        if (relativePath == null) {
            return null;
        }
        return relativePath.isBlank() ? "." : relativePath;
    }

    private static @Nullable ProjectViewPsiSection createDirectoriesSection(Project receiver, String item) {
        PsiFile file = PsiFileFactory
            .getInstance(receiver)
            .createFileFromText(
                "dummy.bazelproject",
                ProjectViewFileType.INSTANCE,
                "directories:\n  " + item + "\n\n"
            );
        ProjectViewPsiSection[] sections = PsiTreeUtil.getChildrenOfType(file, ProjectViewPsiSection.class);
        return sections != null ? sections[0] : null;
    }

    private static @Nullable ProjectViewPsiSectionItem findItemByText(ProjectViewPsiSection receiver, String text) {
        return receiver.getItems().stream().filter(it -> it.getText().equals(text)).findFirst().orElse(null);
    }
}
