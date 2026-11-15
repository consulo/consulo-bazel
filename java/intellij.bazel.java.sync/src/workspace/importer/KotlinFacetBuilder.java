package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.module.Module;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.KotlinBuildTarget;
import org.jetbrains.bazel.workspace.importer.KotlinFacetEntityUpdater.KotlinOptions;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

// RC: replaces the kotlin-facet EP call previously inlined in `JavaModuleUpdater` (the `kind.includesKotlin()` branch
// and `addKotlinModuleIfPossible`); also owns the new `KotlinOptions` bag that replaces `KotlinAddendum`
// consulo: the Kotlin module extension is created by the KotlinFacetEntityUpdater of the Kotlin integration
public final class KotlinFacetBuilder {
    private KotlinFacetBuilder() {
    }

    public static void write(
        @Nullable KotlinBuildTarget kotlinBuildTarget,
        boolean isTestModule,
        Set<String> associates,
        Module parentModuleEntity,
        BazelModelBuilder storage
    ) {
        List<KotlinFacetEntityUpdater> extensions = KotlinFacetEntityUpdater.getEp().getExtensionList();
        if (extensions.isEmpty()) {
            return;
        }
        KotlinFacetEntityUpdater ep = extensions.get(0);
        KotlinOptions kotlinOptions = kotlinBuildTarget == null ? null : new KotlinOptions(
            kotlinBuildTarget.languageVersion(),
            kotlinBuildTarget.apiVersion(),
            kotlinBuildTarget.moduleName(),
            kotlinBuildTarget.kotlincOptions().asList()
        );
        ep.addEntity(
            storage,
            parentModuleEntity,
            kotlinOptions,
            isTestModule,
            associates
        );
    }
}
