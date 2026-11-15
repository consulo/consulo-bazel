package org.jetbrains.bazel.clion.workspace.copts;

import consulo.bazel.cpp.workspace.CcCompilerSwitches;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * An option that forwards other options.
 * <p>
 * consulo: the CIDR {@code CompilerSpecificSwitchBuilder} is {@link CcCompilerSwitches.Builder}
 */
interface CoptsForwarder {

    /**
     * Returns the matching part of the option, or null when the option is not this forwarder.
     */
    @Nullable String match(String option);

    /**
     * Gives the options that {@code option} forwards. It takes a separate value from {@code options}.
     */
    List<String> expand(String option, Iterator<String> options);

    /**
     * Writes the forwarded switches back using {@code flag}.
     */
    void apply(CcCompilerSwitches.Builder builder, String flag, List<String> values);
}

/**
 * A forwarder that gives the next option to another tool.
 */
class CoptsForwardNext implements CoptsForwarder {
    private final String myFlag;
    private final boolean myPrefix;

    CoptsForwardNext(String flag, boolean prefix) {
        myFlag = flag;
        myPrefix = prefix;
    }

    // consulo: overload for the upstream default value of prefix
    CoptsForwardNext(String flag) {
        this(flag, false);
    }

    @Override
    public @Nullable String match(String option) {
        if (option.equals(myFlag)) {
            return myFlag;
        }
        else if (myPrefix && option.startsWith(myFlag)) {
            return option;
        }
        else {
            return null;
        }
    }

    @Override
    public List<String> expand(String option, Iterator<String> options) {
        if (options.hasNext()) {
            return List.of(options.next());
        }
        else {
            return List.of();
        }
    }

    @Override
    public void apply(CcCompilerSwitches.Builder builder, String flag, List<String> values) {
        for (String value : values) {
            builder.withSwitch(flag);
            builder.withSwitch(value);
        }
    }
}

class CoptsForwardCommaSeparated implements CoptsForwarder {
    private final String myFlag;

    private final String myPrefix;

    CoptsForwardCommaSeparated(String flag) {
        myFlag = flag;
        myPrefix = flag + ",";
    }

    @Override
    public @Nullable String match(String option) {
        return option.startsWith(myPrefix) ? myFlag : null;
    }

    @Override
    public List<String> expand(String option, Iterator<String> options) {
        // consulo: Kotlin removePrefix + split(","), which keeps the empty parts
        return Arrays.asList(option.substring(myPrefix.length()).split(",", -1));
    }

    @Override
    public void apply(CcCompilerSwitches.Builder builder, String flag, List<String> values) {
        if (!values.isEmpty()) {
            builder.withSwitch(myPrefix + String.join(",", values));
        }
    }
}
