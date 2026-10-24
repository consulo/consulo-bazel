package org.jetbrains.bazel.sync.workspace.snapshot;

import java.util.List;

/**
 * Definition of bazel configuration
 *
 * @param id        Unique configuration identifier
 * @param summary   Configuration summary taken from BEP
 * @param fragments Bazel configuration fragments
 */
public record WorkspaceConfiguration(
    WorkspaceConfigurationId id,
    WorkspaceConfigurationSummary summary,
    List<WorkspaceConfigurationFragment> fragments
) {
}
