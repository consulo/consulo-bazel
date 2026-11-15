package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import org.jetbrains.bazel.action.SuspendableAction;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consulo: upstream Kotlin top-level function; the class keeps the upstream file name
public final class BazelToolWindowPanel {
    private BazelToolWindowPanel() {
    }

    public static void configureBazelToolWindowToolBar(
        BazelTargetsPanelModel model,
        ActionManager actionManager,
        SimpleToolWindowPanel windowPanel,
        Project project
    ) {
        AnAction defaultActions = actionManager.getAction("Bazel.ActionsToolbar");
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.addAll(defaultActions);
        actionGroup.addSeparator();
        actionGroup.add(new FilterActionGroup(model));
        actionGroup.addSeparator();
        actionGroup.add(new BazelToolWindowSettingsAction(BazelPluginLocalize.projectSettingsDisplayName().get()));
        actionGroup.addSeparator();
        actionGroup.add(actionManager.getAction("Bazel.OpenProjectViewFile"));

        ActionToolbar actionToolbar = actionManager.createActionToolbar("Bazel Toolbar", actionGroup, true);
        actionToolbar.setTargetComponent(windowPanel.getComponent());
        windowPanel.setToolbar(actionToolbar.getComponent());
    }
}

final class BazelToolWindowSettingsAction extends SuspendableAction {
    private final String mySettingsDisplayName;

    BazelToolWindowSettingsAction(String settingsDisplayName) {
        super(
            BazelPluginLocalize.widgetSettingsPopupMessage(settingsDisplayName),
            PlatformIconGroup.generalSettings()
        );
        mySettingsDisplayName = settingsDisplayName;
    }

    @Override
    protected CompletableFuture<Void> actionPerformed(Project project, AnActionEvent e) {
        ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
        // consulo: withContext(Dispatchers.EDT) { writeIntentReadAction { } } -> the project UI access
        return CompletableFuture
            .supplyAsync(() -> showSettingsUtil.showSettingsDialog(project, mySettingsDisplayName), project.getUIAccess())
            .thenCompose(Function.identity());
    }
}
