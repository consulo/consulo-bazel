package org.jetbrains.bazel.languages.projectview.psi;

import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectViewElementFactory {
    // consulo: upstream private top-level constant of this file
    private static final String DUMMY_FILENAME = "dummy.bazelproject";

    private final Project myProject;

    public ProjectViewElementFactory(Project project) {
        myProject = project;
    }

    public ProjectViewPsiSection createSection(String name) {
        return createSection(name, List.of());
    }

    public ProjectViewPsiSection createSection(String name, List<String> items) {
        String itemLines = items.stream().map(it -> "  " + it + "\n").collect(Collectors.joining(""));
        return PsiTreeUtil.getChildrenOfTypeAsList(createFile(name + ":\n" + itemLines), ProjectViewPsiSection.class)
            .get(0);
    }

    public ProjectViewPsiSection createSingleLineSection(String name, String item) {
        return PsiTreeUtil.getChildrenOfTypeAsList(createFile(name + ": " + item + "\n"), ProjectViewPsiSection.class)
            .get(0);
    }

    private PsiFile createFile(String text) {
        return PsiFileFactory
            .getInstance(myProject)
            .createFileFromText(DUMMY_FILENAME, ProjectViewFileType.INSTANCE, text);
    }
}
