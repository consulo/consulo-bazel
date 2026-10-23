package org.jetbrains.bsp.protocol;

import org.jetbrains.bazel.label.Label;

import java.nio.file.Path;
import java.util.List;

record DependencySourcesItem(Label target, List<Path> sources) {
}
