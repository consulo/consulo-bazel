package org.jetbrains.bazel.languages.bazelquery.functions;

import consulo.bazel.documentation.DocumentationSymbol;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelquery.documentation.BazelQueryFunctionDocumentationTarget;

public class BazelQueryFunctionSymbol implements DocumentationSymbol {
    private final BazelQueryFunction myFunction;
    private final Project myProject;

    public BazelQueryFunctionSymbol(BazelQueryFunction function, Project project) {
        myFunction = function;
        myProject = project;
    }

    public BazelQueryFunction getFunction() {
        return myFunction;
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: createPointer()/dereference() are dropped, pointer/presentation are not used by the classic documentation provider

    @Override
    public BazelQueryFunctionDocumentationTarget getDocumentationTarget() {
        return new BazelQueryFunctionDocumentationTarget(this, myProject);
    }
}
