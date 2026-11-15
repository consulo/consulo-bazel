package org.jetbrains.bazel.clion.workspace;

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

import java.util.ArrayList;
import java.util.List;

// TODO consulo: not registered (upstream cidr projectModel.resolveConfigurationChooser, id Bazel.ccRunConfigurationChooser, order
//   "after com.jetbrains.cidr.lang.workspace.RunConfigurationBasedChooser, before Bazel.ccDirectoryBasedChooser"),
//   OCResolveConfigurationChooser / OCResolveConfigurationSlice / OCResolveConfiguration (com.jetbrains.cidr.lang.workspace) do not
//   exist in consulo-cpp, which has no resolve configurations to choose between
public class CcRunConfigurationChooser /* : OCResolveConfigurationChooser */ {
    public CcRunConfigurationChooser() {
    }

    // TODO consulo: OCResolveConfigurationChooser.selectPreselectedResolveConfiguration does not exist in consulo-cpp
    // override fun selectPreselectedResolveConfiguration(
    //   project: Project,
    //   slice: OCResolveConfigurationSlice,
    //   file: VirtualFile?,
    // ): OCResolveConfiguration? {
    //   if (!project.isBazelProject) return null
    //
    //   val labels = selectedLabels(project)
    //   if (labels.isEmpty()) return null
    //
    //   val graph = project.service<WorkspaceSnapshotService>().snapshot.value.targetGraph
    //   return slice.resolve(project).firstOrNull { resolveConfiguration ->
    //     val configurationId = resolveConfiguration.getCcIdentifier()?.configurationId ?: return@firstOrNull false
    //     labels.any { graph.findTargetByKey(WorkspaceTargetKey(it, configurationId)) != null }
    //   }
    // }

    private List<Label> selectedLabels(Project project) {
        RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
        if (!(selected != null && selected.getConfiguration() instanceof BazelRunConfiguration configuration)) {
            return List.of();
        }
        List<Label> result = new ArrayList<>();
        for (Label it : configuration.getTargets()) {
            Label canonical = BazelRepoMappingUtils.toCanonicalLabel(it, project);
            if (canonical != null && !result.contains(canonical)) {
                result.add(canonical);
            }
        }
        return result;
    }
}
