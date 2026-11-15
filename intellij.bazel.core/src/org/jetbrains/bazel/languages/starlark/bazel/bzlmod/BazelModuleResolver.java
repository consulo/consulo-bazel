package org.jetbrains.bazel.languages.starlark.bazel.bzlmod;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelModuleResolver {
    String getId();

    String getName();

    CompletableFuture<@Nullable List<String>> getModuleNames(Project project);

    CompletableFuture<@Nullable List<String>> getModuleVersions(Project project, String moduleName);

    CompletableFuture<?> refreshModuleNames(Project project);

    // consulo: upstream companion EP_NAME is Application.get().getExtensionList(BazelModuleResolver.class)
}
