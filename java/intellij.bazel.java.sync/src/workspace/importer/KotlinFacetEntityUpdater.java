package org.jetbrains.bazel.workspace.importer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.component.extension.ExtensionPoint;
import consulo.module.Module;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

// TODO: should be removed, kotlin needs a separate sync hook: https://youtrack.jetbrains.com/issue/BAZEL-1885
// RC: EP interface moved out of `JavaModuleUpdater` and reshaped to take `KotlinOptions` + `ModuleEntity`
// instead of the old `JavaModule` wrapper
// consulo: the workspace model diff is BazelModelBuilder, the ModuleEntity is the classic Module created by it
@ExtensionAPI(ComponentScope.APPLICATION)
public interface KotlinFacetEntityUpdater {
    record KotlinOptions(
        @Nullable String languageVersion,
        @Nullable String apiVersion,
        @Nullable String moduleName,
        List<String> kotlincOptions
    ) {
    }

    void addEntity(
        BazelModelBuilder diff,
        Module parentModuleEntity,
        @Nullable KotlinOptions kotlinOptions,
        boolean isTestModule,
        Set<String> associates
    );

    // consulo: upstream companion `ep` (ExtensionPointName "org.jetbrains.bazel.kotlinFacetEntityUpdater")
    static ExtensionPoint<KotlinFacetEntityUpdater> getEp() {
        return Application.get().getExtensionPoint(KotlinFacetEntityUpdater.class);
    }
}
