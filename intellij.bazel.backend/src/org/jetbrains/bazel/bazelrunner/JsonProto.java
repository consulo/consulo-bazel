package org.jetbrains.bazel.bazelrunner;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Representations of the messages from
 * https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/build.proto
 * in the JSON form that `--output=streamed_jsonproto` prints.
 */
// consulo: `internal` upstream, used from other packages of the module
public final class JsonProto {
    public record Attribute(String name, @Nullable String stringValue, @Nullable List<String> stringListValue) {
    }

    public record Repository(
        @Nullable String moduleKey,
        @Nullable String canonicalName,
        @Nullable String repoRuleName,
        List<Attribute> attribute
    ) {
    }
}
