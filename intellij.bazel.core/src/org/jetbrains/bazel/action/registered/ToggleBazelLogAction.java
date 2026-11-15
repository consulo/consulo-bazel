package org.jetbrains.bazel.action.registered;

import consulo.annotation.component.ActionImpl;
import consulo.application.util.registry.Registry;
import consulo.bazel.commons.BazelRegistryKeys;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import org.jetbrains.bazel.config.BazelFeatureFlags;

@ActionImpl(id = "Bazel.ToggleBazelLogAction")
public class ToggleBazelLogAction extends DumbAwareToggleAction {
    public ToggleBazelLogAction() {
        super(BazelPluginLocalize.actionBazelTogglebazellogactionText());
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return BazelRegistryKeys.is(BazelFeatureFlags.ENABLE_LOG);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Registry.get(BazelFeatureFlags.ENABLE_LOG).setValue(state);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabledAndVisible(project != null && org.jetbrains.bazel.config.project.isBazelProject(project));
    }

    // consulo: ToggleAction always updates in background (upstream ActionUpdateThread.BGT)
}
