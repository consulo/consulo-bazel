package org.jetbrains.bazel.languages.bazelrc.flags;

// consulo: upstream declares this enum in Annotations.kt (see Option.java)

// Metadata: from com.google.devtools.common.options.proto.OptionFilters.OptionMetadataTag
public enum OptionMetadataTag {
    EXPERIMENTAL,
    INCOMPATIBLE_CHANGE,
    DEPRECATED,
    IMMUTABLE,
    HIDDEN,
}
