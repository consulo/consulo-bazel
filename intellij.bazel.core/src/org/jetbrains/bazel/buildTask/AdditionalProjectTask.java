package org.jetbrains.bazel.buildTask;

import org.jetbrains.bazel.commons.BazelStatus;

import java.util.concurrent.CompletableFuture;

// consulo: upstream declares it in BazelProjectTaskRunner.kt; own file to keep the FQN
public interface AdditionalProjectTask {
    CompletableFuture<?> preRun();

    CompletableFuture<?> postRun(BazelStatus result);
}
