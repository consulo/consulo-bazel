package org.jetbrains.bazel.sync;

import org.jetbrains.bazel.label.Label;

import java.nio.file.Path;
import java.util.List;

public sealed interface ProjectSyncScope {
    /**
     * Invoke bazel build actions
     */
    boolean build();

    /**
     * Perform full resync of the project, discard old state and
     * rebuild it from scratch through bazel invocation over entire target universe
     *
     * @param build  Invoke bazel build actions
     * @param phased Execute as phased sync (query + build)
     */
    record Full(boolean build, boolean phased) implements ProjectSyncScope {
    }

    /**
     * Resync subset of targets specified by {@code patterns}.
     * `bazel build` invocation is done over target patterns included in {@code patterns}.
     * Project model update is purely additive, that means target removal is not handled.
     *
     * @param patterns Target patterns used in `bazel build` invocation
     * @param build    Invoke bazel build actions
     */
    record Targets(List<Label> patterns, boolean build) implements ProjectSyncScope {
    }

    /**
     * Perform project resync based on modified files, {@code files} list can contain
     * both bazel configuration files (e.g., `BUILD`, `MODULE.bazel`, `.bazelproject`, `.bzl`) and normal source files.
     * Based on modified files sync scope might be demoted to full resync.
     * <p>
     * Same as {@link ProjectSyncScope.Targets} scope project model update is additive.
     *
     * @param files Modified files
     * @param build Invoke bazel build actions
     */
    record Files(List<Path> files, boolean build) implements ProjectSyncScope {
    }

    ///**
    // * Perform incremental sync over changes targets relative to previous sync run.
    // * In contrast to [ProjectSyncScope.Targets] and [ProjectSyncScope.Files] this mode
    // * handle removal of targets from project model.
    // *
    // * @property build Invoke bazel build actions
    // */
    //data class Incremental(val build: Boolean) : ProjectSyncScope
}
