package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.jetbrains.bazel.languages.projectview.ProjectViewExtensions.BUILD_FLAGS_KEY;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class setCompilationMode {
    private setCompilationMode() {
    }

    private static final Pattern COMPILATION_MODE_FLAG = Pattern.compile("^(-c|--compilation_mode)([= ]\\S+)?$");

    /**
     * Sets {@code --compilation_mode=[mode]} after all `build_flags` sections and imports of the file.
     * Removes every other compilation mode flag from the `build_flags` sections of the file.
     * Appends the flag to the last `build_flags` section when no import follows it. Otherwise,
     * adds a new section at the end of the file.
     */
    public static void setCompilationMode(ProjectViewPsiFile receiver, String mode) {
        for (ProjectViewPsiSection section : PsiTreeUtil.getChildrenOfTypeAsList(receiver, ProjectViewPsiSection.class)) {
            if (!isBuildFlagsSection(section)) {
                continue;
            }
            List<ProjectViewPsiSectionItem> items = section.getItems()
                .stream()
                .filter(it -> COMPILATION_MODE_FLAG.matcher(it.getText().trim()).matches())
                .toList();
            items.forEach(it -> removeItem(section, it));
        }

        String item = "--compilation_mode=" + mode;
        ProjectViewPsiSection buildFlags = lastBuildFlagsContributor(receiver) instanceof ProjectViewPsiSection section ? section : null;

        if (buildFlags == null) {
            ProjectViewPsiSection newSection = createBuildFlagsSection(receiver, item);
            PsiElement lastChild = receiver.getLastChild();
            if (lastChild != null && !lastChild.getText().endsWith("\n")) {
                PsiElement newLine = createNewLine(receiver);
                if (newLine == null) {
                    return;
                }
                receiver.add(newLine);
            }
            receiver.add(newSection);
            return;
        }

        ProjectViewPsiSection newSection = createBuildFlagsSection(receiver, item);
        PsiElement colon = newSection.getColon();
        if (colon == null) {
            return;
        }
        List<ProjectViewPsiSectionItem> buildFlagsItems = buildFlags.getItems();
        PsiElement anchor = !buildFlagsItems.isEmpty() ? buildFlagsItems.getLast() : buildFlags.getColon();
        // consulo: Kotlin platform types
        buildFlags.addRangeAfter(Objects.requireNonNull(colon.getNextSibling()), newSection.getItems().getFirst(), Objects.requireNonNull(anchor));
    }

    /**
     * Returns the offset where {@link #setCompilationMode} writes the flag.
     * This is the end of the last `build_flags` section when no import follows it. Otherwise, it is the end of the file.
     */
    public static int buildFlagsEndOffset(ProjectViewPsiFile receiver) {
        if (!(lastBuildFlagsContributor(receiver) instanceof ProjectViewPsiSection buildFlags)) {
            return receiver.getTextLength();
        }
        List<ProjectViewPsiSectionItem> items = buildFlags.getItems();
        PsiElement colon = buildFlags.getColon();
        PsiElement element = !items.isEmpty() ? items.getLast() : colon != null ? colon : buildFlags;
        return element.getTextRange().getEndOffset();
    }

    /**
     * Returns the last `build_flags` section or import of the file. An import can supply `build_flags` too.
     */
    private static @Nullable PsiElement lastBuildFlagsContributor(ProjectViewPsiFile receiver) {
        PsiElement[] children = receiver.getChildren();
        for (int i = children.length - 1; i >= 0; i--) {
            PsiElement it = children[i];
            if (it instanceof ProjectViewPsiImportBase || it instanceof ProjectViewPsiSection section && isBuildFlagsSection(section)) {
                return it;
            }
        }
        return null;
    }

    private static boolean isBuildFlagsSection(ProjectViewPsiSection section) {
        return ProjectViewPsiSection.sectionName(section).equals(BUILD_FLAGS_KEY.getName());
    }

    private static void removeItem(ProjectViewPsiSection section, ProjectViewPsiSectionItem item) {
        PsiElement prevSibling = item.getPrevSibling();
        PsiElement from = prevSibling != null && PsiTreeUtil.isAncestor(section, prevSibling, false) ? prevSibling : section.getColon();
        PsiElement nextSibling = item.getNextSibling();
        PsiElement to = nextSibling != null && PsiTreeUtil.isAncestor(section, nextSibling, false) ? nextSibling : section.getLastChild();
        section.deleteChildRange(Objects.requireNonNull(from), Objects.requireNonNull(to));
    }

    private static ProjectViewPsiSection createBuildFlagsSection(ProjectViewPsiFile receiver, String item) {
        return new ProjectViewElementFactory(receiver.getProject()).createSection(BUILD_FLAGS_KEY.getName(), List.of(item));
    }

    private static @Nullable PsiElement createNewLine(ProjectViewPsiFile receiver) {
        return createDummyFile(receiver, "\n").getFirstChild();
    }

    private static PsiFile createDummyFile(ProjectViewPsiFile receiver, String text) {
        return PsiFileFactory
            .getInstance(receiver.getProject())
            .createFileFromText("dummy.bazelproject", ProjectViewFileType.INSTANCE, text);
    }
}
