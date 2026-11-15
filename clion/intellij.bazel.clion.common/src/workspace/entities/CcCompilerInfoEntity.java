package org.jetbrains.bazel.clion.workspace.entities;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * consulo: upstream Kotlin declarations of CcCompilerInfoEntity.kt. The workspace entity is a record, stored as
 * project data of the {@code BazelModelBuilder} (see CONSULO.md, "Project model").
 */
public final class CcCompilerInfoEntity {
    private CcCompilerInfoEntity() {
    }

    public record CcToolchainCompilerInfoEntity(
        WorkspaceModelTargetKey _toolchainKey,
        String cCompilerPath,
        String cCompilerKindId,
        String cppCompilerPath,
        String cppCompilerKindId,
        List<String> cSwitches,
        List<String> cppSwitches,
        String compilerName,
        Map<String, String> environment,
        List<OutputLocation> builtinIncludes,
        @Nullable OutputLocation sysroot
    ) {
    }

    /**
     * consulo: all entities of one sync, the project data of the builder (upstream {@code storage.entities(...)})
     */
    public record CcToolchainCompilerInfoEntities(List<CcToolchainCompilerInfoEntity> entities) {
        public CcToolchainCompilerInfoEntities {
            entities = List.copyOf(entities);
        }
    }

    public static WorkspaceTargetKey getToolchainKey(CcToolchainCompilerInfoEntity receiver) {
        return receiver._toolchainKey().toWorkspaceTarget();
    }
}
