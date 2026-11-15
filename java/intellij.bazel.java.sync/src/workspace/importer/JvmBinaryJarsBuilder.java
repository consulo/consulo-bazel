package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.module.Module;
import org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity;

import java.nio.file.Path;
import java.util.List;

// RC: replaces `JvmBinaryJarsEntityUpdater`
public final class JvmBinaryJarsBuilder {
    private JvmBinaryJarsBuilder() {
    }

    // consulo: the JvmBinaryJarsEntity data is attached to its module with BazelModelBuilder.putModuleData
    public static void write(
        List<Path> binaryJars,
        Module parentModuleEntity,
        BazelModelBuilder storage
    ) {
        if (binaryJars.isEmpty()) {
            return;
        }
        JvmBinaryJarsEntity entity =
            new JvmBinaryJarsEntity(
                binaryJars.stream().map(BuilderUtils::toVirtualFileUrl).toList()
            );
        storage.putModuleData(parentModuleEntity, JvmBinaryJarsEntity.class, entity);
    }
}
