package org.jetbrains.bazel.languages.starlark.documentation;

import consulo.bazel.documentation.DocumentationSymbol;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunctionParameter;

public class BazelGlobalFunctionArgumentDocumentationSymbol implements DocumentationSymbol {
    private final BazelGlobalFunctionParameter myArgument;
    private final Project myProject;

    public BazelGlobalFunctionArgumentDocumentationSymbol(BazelGlobalFunctionParameter argument, Project project) {
        myArgument = argument;
        myProject = project;
    }

    public BazelGlobalFunctionParameter getArgument() {
        return myArgument;
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: createPointer()/dereference() (the symbol was its own Pointer) are dropped,
    // pointer/presentation are not used by the classic documentation provider

    @Override
    public BazelGlobalFunctionArgumentDocumentationTarget getDocumentationTarget() {
        return new BazelGlobalFunctionArgumentDocumentationTarget(this);
    }
}
