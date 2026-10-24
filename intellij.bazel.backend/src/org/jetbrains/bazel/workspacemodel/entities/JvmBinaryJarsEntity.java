package org.jetbrains.bazel.workspacemodel.entities;

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.module.Module;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * consulo: classic data instead of a workspace entity, attached to its module with
 * {@code BazelModelBuilder.putModuleData}; {@code jars} are VFS urls.
 */
public record JvmBinaryJarsEntity(List<String> jars) {
    public static @Nullable JvmBinaryJarsEntity getJvmBinaryJarsEntity(Module module) {
        return BazelProjectModelService.getInstance(module.getProject()).getModuleData(module, JvmBinaryJarsEntity.class);
    }
}
