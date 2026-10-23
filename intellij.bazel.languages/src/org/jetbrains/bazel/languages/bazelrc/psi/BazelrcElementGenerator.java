package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage;

import java.util.Objects;

public class BazelrcElementGenerator {
    private static final String DUMMY_FILENAME = "dummy.bazelrc";

    private final Project myProject;

    public BazelrcElementGenerator(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    private BazelrcFile createDummyFile(String contents) {
        PsiFileFactory factory = PsiFileFactory.getInstance(myProject);

        // consulo: upstream creates a LightVirtualFile(DUMMY_FILENAME, BazelrcFileType, contents)
        // and calls PsiFileFactoryImpl.trySetupPsiForFile(virtualFile, BazelrcLanguage, false, true)
        return (BazelrcFile) factory.createFileFromText(DUMMY_FILENAME, BazelrcLanguage.INSTANCE, contents, false, true);
    }

    public PsiElement createFlagName(String flagName) {
        return Objects.requireNonNull(createDummyFile("common --" + flagName).getLines()[0].getFlags()[0].getNameElement());
    }
}
