package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.bazel.documentation.DocumentationSymbol;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;

public class BazelGlobalFunctionDocumentationSymbol implements DocumentationSymbol {
    private final BazelGlobalFunction myFunction;
    private final Project myProject;

    public BazelGlobalFunctionDocumentationSymbol(BazelGlobalFunction function, Project project) {
        myFunction = function;
        myProject = project;
    }

    public BazelGlobalFunction getFunction() {
        return myFunction;
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: createPointer()/dereference() (the symbol was its own Pointer) are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public BazelGlobalFunctionDocumentationTarget getDocumentationTarget() {
        return new BazelGlobalFunctionDocumentationTarget(this);
    }
}
