package org.jetbrains.bazel.workspace;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelExecutableProvider {
    CompletableFuture<@Nullable Path> computeBazelExecutable(Project project);

    static ExtensionPoint<BazelExecutableProvider> getEp() {
        return Application.get().getExtensionPoint(BazelExecutableProvider.class);
    }

    static CompletableFuture<Path> computeBazelExecutableOrFail(Project project) {
        return computeBazelExecutableOrFail(project, getEp().iterator());
    }

    // consulo: the upstream sequential loop over the providers, as a chain of futures
    private static CompletableFuture<Path> computeBazelExecutableOrFail(Project project, Iterator<BazelExecutableProvider> providers) {
        if (!providers.hasNext()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Failed to find bazel executable: 'bazel'/'bazelisk' is not on PATH and bazelisk could not be downloaded (see idea.log)"
            ));
        }
        BazelExecutableProvider provider = providers.next();
        return provider.computeBazelExecutable(project).thenCompose(executable -> {
            if (executable != null) {
                return CompletableFuture.completedFuture(executable);
            }
            return computeBazelExecutableOrFail(project, providers);
        });
    }
}
