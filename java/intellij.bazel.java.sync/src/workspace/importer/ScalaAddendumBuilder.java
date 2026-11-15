package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.module.Module;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Function;

// RC: replaces `ScalaAddendumEntityUpdater`; the old `ScalaAddendum` wrapper is dropped,
// we go straight from `ScalaBuildTarget` to `ScalaAddendumEntity`
public final class ScalaAddendumBuilder {
    private ScalaAddendumBuilder() {
    }

    // consulo: the ScalaAddendumEntity data is attached to its module with BazelModelBuilder.putModuleData
    public static void write(
        @Nullable ScalaBuildTarget scalaBuildTarget,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        Module parentModuleEntity,
        BazelModelBuilder storage
    ) {
        ScalaBuildTarget target = scalaBuildTarget;
        if (target == null) {
            return;
        }
        ScalaAddendumEntity entity =
            new ScalaAddendumEntity(
                target.scalaVersion(),
                target.scalacOptions().asList(),
                JvmTargetEntitiesBuilder.resolvePaths(target.sdkJars(), resolveLocation).stream().map(BuilderUtils::toVirtualFileUrl).toList()
            );
        storage.putModuleData(parentModuleEntity, ScalaAddendumEntity.class, entity);
    }
}
