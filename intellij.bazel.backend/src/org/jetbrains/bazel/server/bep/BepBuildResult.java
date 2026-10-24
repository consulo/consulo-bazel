package org.jetbrains.bazel.server.bep;

import org.jetbrains.bazel.bazelrunner.BazelProcessResult;

public record BepBuildResult(BazelProcessResult processResult, BepOutput bepOutput) {
}
