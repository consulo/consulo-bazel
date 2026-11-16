package org.jetbrains.bazel.java.compiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavacOptionsUpdater;

import java.util.Map;

@ExtensionImpl
public class BazelJavacOptionsUpdater implements JavacOptionsUpdater {
    public BazelJavacOptionsUpdater() {
    }

    @Override
    public void updateJavacOptions(Project project, Map<String, String> optionsByModuleName) {
        // consulo: upstream sets JavacConfiguration.ADDITIONAL_OPTIONS_OVERRIDE = HashMap(optionsByModuleName) (the settings UI mutates
        //  this map in place). Consulo has no per-module javac options override; the options are the compiler arguments of the Java
        //  module extension, which JavaBazelWorkspaceImporter.applyJavacOptions already sets on the root models during the sync
    }
}
