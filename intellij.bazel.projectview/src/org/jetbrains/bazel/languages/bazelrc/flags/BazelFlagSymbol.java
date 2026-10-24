package org.jetbrains.bazel.languages.bazelrc.flags;

import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget;

// consulo: DocumentationSymbol and Pointer (IntelliJ symbol API) do not exist in Consulo, the symbol is a plain class
public class BazelFlagSymbol {
    private final Flag myFlag;
    private final Project myProject;

    public BazelFlagSymbol(Flag flag, Project project) {
        myFlag = flag;
        myProject = project;
    }

    public Flag getFlag() {
        return myFlag;
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: createPointer()/dereference() are dropped, pointer/presentation are not used by the classic documentation provider

    public BazelFlagDocumentationTarget getDocumentationTarget() {
        return new BazelFlagDocumentationTarget(this);
    }
}
