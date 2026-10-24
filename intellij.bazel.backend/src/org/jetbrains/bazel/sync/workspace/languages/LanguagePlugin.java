package org.jetbrains.bazel.sync.workspace.languages;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot.WorkspaceSyncConfig;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguagePlugin {
    Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes();

    Set<LanguageClass> getSupportedLanguages();

    default List<LanguageClass> collectUsedLanguages(IntellijIdeInfo.TargetIdeInfo target) {
        return List.of();
    }

    /**
     * Create a set of sync configurations that shall be later consumed by {@link BazelWorkspaceImporter}.
     * This is the only time when {@link BazelWorkspaceImporter} can indirectly access {@link ProjectView}.
     * Or any other external data outside {@link ProjectView}
     *
     * @param project {@link Project} that can be used as a data source
     * @param projectView current project view
     */
    default CompletableFuture<List<WorkspaceSyncConfig>> createSyncConfigs(Project project, ProjectView projectView) {
        return CompletableFuture.completedFuture(List.of());
    }

    CompletableFuture<List<BuildTargetData>> mapBuildTargetData(
        BazelServerFacade server,
        IntellijIdeInfo.TargetIdeInfo target
    );

    static ExtensionPoint<LanguagePlugin> getEpName() {
        return Application.get().getExtensionPoint(LanguagePlugin.class);
    }

    class LanguageProjectMappers {
        private final Map<LanguageClass, LanguagePlugin> myRegistry;

        public LanguageProjectMappers(Map<LanguageClass, LanguagePlugin> registry) {
            myRegistry = registry;
        }

        public Map<LanguageClass, LanguagePlugin> getRegistry() {
            return myRegistry;
        }

        public Collection<LanguagePlugin> all() {
            return new LinkedHashSet<>(myRegistry.values());
        }

        public LanguagePlugin get(LanguageClass lang) {
            LanguagePlugin plugin = myRegistry.get(lang);
            if (plugin == null) {
                throw new java.util.NoSuchElementException("Key " + lang + " is missing in the map.");
            }
            return plugin;
        }
    }

    static LanguageProjectMappers createLanguageProjectMappers() {
        Map<LanguageClass, LanguagePlugin> result = new HashMap<>();
        getEpName().forEachExtensionSafe(plugin -> {
            for (LanguageClass languageClass : plugin.getSupportedLanguages()) {
                if (result.containsKey(languageClass)) {
                    throw new IncorrectOperationException("Language class " + languageClass + " is already mapped by another plugin");
                }
                result.put(languageClass, plugin);
            }
        });
        return new LanguageProjectMappers(result);
    }
}
