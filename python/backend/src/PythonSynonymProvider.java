package com.intellij.bazel.python.backend;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Use this extension point if resolving Python imports require a custom change in their qualified names
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PythonSynonymProvider {
    /**
     * Creates a synonym for a Python import.
     * Qualified name returned from here (a "synonym") is searched in the project root when the original name was not found.
     * For example, if in Pythin code a library is referenced as {@code aaa.bbb}, but its file system location is {@code src/python/aaa/bbb.py},
     * then the override might look as follows:
     * <pre>
     * return listOf("src", "python") + qualifiedNameComponents
     * </pre>
     * <p>
     * If for a given qualified name, no synonym should be provided, return {@code null}
     */
    @Nullable List<String> getSynonym(List<String> qualifiedNameComponents);

    // consulo: upstream companion `val ep = ExtensionPointName.create("org.jetbrains.bazel.python.synonymProvider")`
    static List<PythonSynonymProvider> getEp() {
        return Application.get().getExtensionList(PythonSynonymProvider.class);
    }
}
