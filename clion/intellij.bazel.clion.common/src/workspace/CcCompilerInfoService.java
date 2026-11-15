package org.jetbrains.bazel.clion.workspace;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity.CcToolchainCompilerInfoEntities;
import org.jetbrains.bazel.clion.workspace.entities.CcCompilerInfoEntity.CcToolchainCompilerInfoEntity;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class CcCompilerInfoService {
    private final Project myProject;

    // consulo: upstream caches by the workspace model snapshot (EntityStorage); here by the project data object of the
    // last sync, which BazelProjectModelService replaces on every sync
    private record Cached(@Nullable CcToolchainCompilerInfoEntities storage, Map<WorkspaceTargetKey, CcCompilerInfo> target2Compiler) {
    }

    private volatile @Nullable Cached myCached = null;

    @Inject
    public CcCompilerInfoService(Project project) {
        myProject = project;
    }

    public @Nullable CcCompilerInfo get(WorkspaceTargetKey key) {
        return all().get(key);
    }

    public Map<WorkspaceTargetKey, CcCompilerInfo> all() {
        @Nullable CcToolchainCompilerInfoEntities storage =
            BazelProjectModelService.getInstance(myProject).getProjectData(CcToolchainCompilerInfoEntities.class);

        @Nullable Cached cached = myCached;
        if (cached != null && cached.storage() == storage) {
            return cached.target2Compiler();
        }

        Map<WorkspaceTargetKey, CcCompilerInfo> target2Compiler = loadCompilerSettings(storage);
        myCached = new Cached(storage, target2Compiler);

        return target2Compiler;
    }

    public static CcCompilerInfoService getInstance(Project project) {
        return project.getInstance(CcCompilerInfoService.class);
    }

    // consulo: upstream private top-level functions of CcCompilerInfoService.kt

    private static Map<WorkspaceTargetKey, CcCompilerInfo> loadCompilerSettings(@Nullable CcToolchainCompilerInfoEntities storage) {
        List<CcToolchainCompilerInfoEntity> entities = storage == null ? List.of() : storage.entities();
        Map<WorkspaceTargetKey, CcCompilerInfo> result = new LinkedHashMap<>();
        for (CcToolchainCompilerInfoEntity entity : entities) {
            result.put(CcCompilerInfoEntity.getToolchainKey(entity), toCompilerInfo(entity));
        }
        return result;
    }

    private static CcCompilerKind compilerKindById(String id) {
        // consulo: the CIDR OCCompilerId.toString() values
        if (id.equals("CLANG")) {
            return CcCompilerKind.CLANG;
        }
        else if (id.equals("GCC")) {
            return CcCompilerKind.GCC;
        }
        else {
            return CcCompilerKind.UNKNOWN;
        }
    }

    private static CcCompilerInfo toCompilerInfo(CcToolchainCompilerInfoEntity receiver) {
        return new CcCompilerInfo(
            Path.of(receiver.cCompilerPath()),
            compilerKindById(receiver.cCompilerKindId()),
            Path.of(receiver.cppCompilerPath()),
            compilerKindById(receiver.cppCompilerKindId()),
            List.copyOf(receiver.cSwitches()),
            List.copyOf(receiver.cppSwitches()),
            receiver.compilerName(),
            new LinkedHashMap<>(receiver.environment()),
            OutputLocationCollectionBuilder.ofLocations(receiver.builtinIncludes()),
            receiver.sysroot()
        );
    }
}
