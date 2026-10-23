package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;

import java.util.List;

record DependencySourcesParams(List<Label> targets) {
}
