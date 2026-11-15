package org.jetbrains.bazel.target;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.extensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// consulo: the IntelliJ workspace model is not used: modules are classic modules with the BazelModuleExtensionEntity data of
// BazelProjectModelService, and the index is a CachedValue that depends on the project roots. The upstream `snapshot` parameters
// (ImmutableEntityStorage, default currentSnapshot) are dropped, and ModuleEntity results are Modules
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class ModuleTargetService {
    private final Project myProject;

    @Inject
    public ModuleTargetService(Project project) {
        myProject = project;
    }

    private record Indexes(
        Map<Label, List<Module>> modulesByLabel,
        Map<WorkspaceTargetKey, List<Module>> modulesByKey
    ) {
    }

    private Indexes computeIndexes() {
        Map<Label, List<Module>> byLabel = new HashMap<>();
        Map<WorkspaceTargetKey, List<Module>> byKey = new HashMap<>();
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            extensions.BazelModuleExtensionEntity extension = extensions.getBazelModuleExtension(module);
            if (extension == null) {
                continue;
            }
            WorkspaceTargetKey key = extension.getTargetKey();
            byLabel.computeIfAbsent(key.label(), it -> new ArrayList<>()).add(module);
            byKey.computeIfAbsent(key, it -> new ArrayList<>()).add(module);
        }
        return new Indexes(byLabel, byKey);
    }

    private Indexes getIndexes() {
        return CachedValuesManager.getManager(myProject).getCachedValue(
            myProject,
            () -> CachedValueProvider.Result.create(computeIndexes(), ProjectRootManager.getInstance(myProject))
        );
    }

    public List<Module> findModulesByLabel(Label label) {
        List<Module> modules = getIndexes().modulesByLabel().get(label);
        return modules == null ? List.of() : List.copyOf(modules);
    }

    public List<Module> findLegacyModulesByLabel(Label label) {
        return findModulesByLabel(label);
    }

    public List<Module> findModulesByKey(
        WorkspaceTargetKey key
    ) {
        List<Module> modules = getIndexes().modulesByKey().get(key);
        return modules == null ? List.of() : List.copyOf(modules);
    }

}
