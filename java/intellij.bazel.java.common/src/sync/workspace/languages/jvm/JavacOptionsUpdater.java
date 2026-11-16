package org.jetbrains.bazel.sync.workspace.languages.jvm;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import java.util.Map;

/**
 * Stores the per-module javac options computed by the Bazel sync, so that the IDE Java compiler uses them.
 * Without an implementation the options are dropped. The Language Server has none: it never runs a javac build.
 */
// consulo: upstream ExtensionPointName `ep` is replaced by Application.get().getExtensionList(JavacOptionsUpdater.class)
@ExtensionAPI(ComponentScope.APPLICATION)
public interface JavacOptionsUpdater {
    /**
     * @param optionsByModuleName javac options joined by spaces, keyed by module name
     */
    void updateJavacOptions(Project project, Map<String, String> optionsByModuleName);
}
