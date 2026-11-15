package org.jetbrains.bazel.run;

import consulo.process.ProcessHandler;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

// TODO consulo: ProcessHandlerPidProvider (execution.processHandlerPidProvider) is missing in Consulo, the provider is not registered
class BazelProcessHandlerPidProvider {
    public @Nullable CompletableFuture<@Nullable Long> getPid(ProcessHandler processHandler) {
        return processHandler instanceof BazelProcessHandler bazelProcessHandler ? bazelProcessHandler.getPid() : null;
    }
}
