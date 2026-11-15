package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity.CcToolchainCompilerInfoEntities;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity.CcToolchainCompilerInfoEntity;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * consulo: upstream Kotlin top-level declarations of workspaceModule.kt, on the classic project model: the module
 * entity is a module of the {@link BazelModelBuilder}, the compiler info entities are its project data.
 */
public final class workspaceModule {
    private workspaceModule() {
    }

    public static final String CC_WORKSPACE_MODULE_NAME = ".cc-workspace";

    public static void addCcWorkspaceModule(BazelModelBuilder builder, BazelEntitySource entitySource) {
        Module module = builder.getOrCreateModule(CC_WORKSPACE_MODULE_NAME, null);
        // consulo: the root model is cleared on first access; ModuleSourceDependency is implicit in Consulo
        builder.getRootModel(module);
    }

    /**
     * {@code null} before the sync applies the project model, and for a project that Bazel did not import.
     * <p>
     * consulo: upstream returns the {@code ModuleId}, here the module name
     */
    public static @Nullable String findCcWorkspaceModuleId(Project project) {
        String id = CC_WORKSPACE_MODULE_NAME;
        return ModuleManager.getInstance(project).findModuleByName(id) != null ? id : null;
    }

    /**
     * Persists the compiler settings of every resolved toolchain.
     * <p>
     * consulo: the entities are project data of the builder, kept in memory until the next sync
     */
    public static void addCcCompilerInfoEntities(
        BazelModelBuilder builder,
        BazelEntitySource entitySource,
        Map<WorkspaceTargetKey, CcCompilerInfo> toolchain2Compiler
    ) {
        List<CcToolchainCompilerInfoEntity> entities = new ArrayList<>();
        for (Map.Entry<WorkspaceTargetKey, CcCompilerInfo> entry : toolchain2Compiler.entrySet()) {
            CcCompilerInfo info = entry.getValue();
            entities.add(
                new CcToolchainCompilerInfoEntity(
                    WorkspaceModelTargetKey.of(entry.getKey()),
                    info.cCompiler().toString(),
                    String.valueOf(info.cCompilerKind().getId()),
                    info.cppCompiler().toString(),
                    String.valueOf(info.cppCompilerKind().getId()),
                    info.cSwitches(),
                    info.cppSwitches(),
                    info.name(),
                    info.environment(),
                    info.builtinIncludes().getOutputLocations().toList(),
                    info.sysroot()
                )
            );
        }
        builder.putProjectData(CcToolchainCompilerInfoEntities.class, new CcToolchainCompilerInfoEntities(entities));
    }
}
