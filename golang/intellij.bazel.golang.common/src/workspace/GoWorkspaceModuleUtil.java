package org.jetbrains.bazel.golang.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import org.jetbrains.bazel.buildTask.TargetsToBuildProvider;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.BazelGoTargetEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.ImportPathId;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class GoWorkspaceModuleUtil {
    private GoWorkspaceModuleUtil() {
    }

    // consulo: upstream internal top-level const of GoWorkspaceModuleUtil.kt
    public static final String GO_WORKSPACE_MODULE_NAME = ".workspace";

    public static @Nullable Module findModule(Project project) {
        return ModuleManager.getInstance(project).findModuleByName(GO_WORKSPACE_MODULE_NAME);
    }

    /**
     * consulo: the IntelliJ workspace model is not available. The upstream {@code BazelGoPackageEntity} and
     * {@code BazelGoTargetEntity} entities of the project are kept together as project data of the sync
     * ({@code BazelModelBuilder.putProjectData}, read back through {@link BazelProjectModelService}). This class replaces the
     * workspace model queries of upstream: {@code entities<BazelGoPackageEntity>()}, {@code resolve(ImportPathId)},
     * {@code contains(ImportPathId)}, {@code referrers(ImportPathId, BazelGoTargetEntity)} and the virtual file url index.
     */
    public static final class GoPackageEntities {
        public static final GoPackageEntities EMPTY = new GoPackageEntities(List.of(), List.of());

        private final List<BazelGoPackageEntity> myPackages;
        private final List<BazelGoTargetEntity> myTargets;
        private final Map<ImportPathId, BazelGoPackageEntity> myPackagesById;
        private final Map<String, BazelGoPackageEntity> myPackagesBySourceUrl;

        public GoPackageEntities(List<BazelGoPackageEntity> packages, List<BazelGoTargetEntity> targets) {
            myPackages = List.copyOf(packages);
            myTargets = List.copyOf(targets);
            Map<ImportPathId, BazelGoPackageEntity> packagesById = new LinkedHashMap<>();
            Map<String, BazelGoPackageEntity> packagesBySourceUrl = new HashMap<>();
            for (BazelGoPackageEntity it : myPackages) {
                packagesById.putIfAbsent(it.getSymbolicId(), it);
                for (String source : it.sources()) {
                    packagesBySourceUrl.putIfAbsent(source, it);
                }
            }
            myPackagesById = packagesById;
            myPackagesBySourceUrl = packagesBySourceUrl;
        }

        public static GoPackageEntities getInstance(Project project) {
            GoPackageEntities entities = BazelProjectModelService.getInstance(project).getProjectData(GoPackageEntities.class);
            return entities == null ? EMPTY : entities;
        }

        public List<BazelGoPackageEntity> getPackages() {
            return myPackages;
        }

        public List<BazelGoTargetEntity> getTargets() {
            return myTargets;
        }

        public @Nullable BazelGoPackageEntity resolve(ImportPathId id) {
            return myPackagesById.get(id);
        }

        public boolean contains(ImportPathId id) {
            return myPackagesById.containsKey(id);
        }

        public Stream<BazelGoTargetEntity> referrers(ImportPathId id) {
            return myTargets.stream().filter(it -> it.importPath().equals(id));
        }

        /**
         * The package whose {@code sources} contain the file with the given VFS url.
         */
        public @Nullable BazelGoPackageEntity findBySourceUrl(String url) {
            return myPackagesBySourceUrl.get(url);
        }
    }
}

@ExtensionImpl
class GoTargetsToBuildProvider implements TargetsToBuildProvider {
    public GoTargetsToBuildProvider() {
    }

    @Override
    public List<Label> getTargetsToBuild(Project project, Module moduleEntity) {
        if (!moduleEntity.getName().equals(GoWorkspaceModuleUtil.GO_WORKSPACE_MODULE_NAME)) {
            return List.of();
        }
        // consulo: upstream reads the BazelGoTargetEntity entities of the workspace model
        return GoWorkspaceModuleUtil.GoPackageEntities.getInstance(project).getTargets().stream()
            .map(it -> it.getTargetKey().label())
            .toList();
    }
}
