package org.jetbrains.bazel.languages.bazelversion.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelVersionResolver {
    String getId();

    String getName();

    CompletableFuture<@Nullable BazelVersionLiteral> resolveLatestBazelVersion(
        Project project,
        @Nullable BazelVersionLiteral currentVersion
    );

    // consulo: upstream ExtensionPointName "org.jetbrains.bazel.bazelVersionResolver"
    static ExtensionPoint<BazelVersionResolver> getEp() {
        return Application.get().getExtensionPoint(BazelVersionResolver.class);
    }
}
