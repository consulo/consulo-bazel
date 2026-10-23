package org.jetbrains.bazel.performance;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class BspTracer {
    private BspTracer() {
    }

    public static <T> CompletableFuture<T> measure(String description, Supplier<CompletableFuture<T>> body) {
        // consulo: telemetry is not ported, measure() only runs the body
        return body.get();
    }
}
