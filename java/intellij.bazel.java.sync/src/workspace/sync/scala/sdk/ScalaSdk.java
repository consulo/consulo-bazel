package org.jetbrains.bazel.scala.sdk;

import java.net.URI;
import java.util.List;

public record ScalaSdk(
    String name,
    String scalaVersion,
    List<URI> sdkJars
) {
}
