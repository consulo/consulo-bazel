package org.jetbrains.bazel.util;

import java.time.Duration;

// consulo: `internal` upstream, used from other packages of the module
public final class Stopwatch {
    private final long myStart = now();

    private Stopwatch() {
    }

    public Duration stop() {
        return Duration.ofMillis(now() - myStart);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public static Stopwatch start() {
        return new Stopwatch();
    }
}
