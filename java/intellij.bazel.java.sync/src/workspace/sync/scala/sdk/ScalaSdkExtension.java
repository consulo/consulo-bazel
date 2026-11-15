package org.jetbrains.bazel.scala.sdk;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.bazel.workspace.BazelModelBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * this API will be sunset for the {@link org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity} later,
 * once Scala plugin is compatible with WSM
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ScalaSdkExtension {
    // consulo: IntelliJ IdeModifiableModelsProvider does not exist, the classic models are provided by BazelModelBuilder
    void addScalaSdk(ScalaSdk scalaSdk, BazelModelBuilder modelsProvider);

    // consulo: upstream private top-level val ep (ExtensionPointName "org.jetbrains.bazel.scalaSdkExtension")
    private static List<ScalaSdkExtension> ep() {
        return Application.get().getExtensionList(ScalaSdkExtension.class);
    }

    static @Nullable ScalaSdkExtension scalaSdkExtension() {
        List<ScalaSdkExtension> extensions = ep();
        return extensions.isEmpty() ? null : extensions.get(0);
    }

    static boolean scalaSdkExtensionExists() {
        return !ep().isEmpty();
    }
}
