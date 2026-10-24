package org.jetbrains.bazel.languages.bazelrc.flags;

// consulo: upstream declares this enum in Annotations.kt (see Option.java)

// Known effects: from com.google.devtools.common.options.proto.OptionFilters.OptionEffectTag
public enum OptionEffectTag {
    NO_OP,
    LOSES_INCREMENTAL_STATE,
    CHANGES_INPUTS,
    AFFECTS_OUTPUTS,
    BUILD_FILE_SEMANTICS,
    BAZEL_INTERNAL_CONFIGURATION,
    LOADING_AND_ANALYSIS,
    EXECUTION,
    HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
    EAGERNESS_TO_EXIT,
    BAZEL_MONITORING,
    TERMINAL_OUTPUT,
    ACTION_COMMAND_LINES,
    TEST_RUNNER,
    UNKNOWN,
}
