package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.commons.BazelStatus;

public record CompileResult(BazelStatus statusCode) {
}
