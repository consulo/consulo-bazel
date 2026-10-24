package org.jetbrains.bazel.workspacemodel.entities;

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.module.Module;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * consulo: classic data instead of a workspace entity, attached to its module with
 * {@code BazelModelBuilder.putModuleData}; {@code sdkClasspaths} are VFS urls.
 */
public record ScalaAddendumEntity(
    String compilerVersion,
    List<String> scalacOptions,
    List<String> sdkClasspaths
) {
    public static @Nullable ScalaAddendumEntity getScalaAddendumEntity(Module module) {
        return BazelProjectModelService.getInstance(module.getProject()).getModuleData(module, ScalaAddendumEntity.class);
    }
}
