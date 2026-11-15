package org.jetbrains.bazel.ui.widgets.fileTargets;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.fileEditor.statusBar.EditorBasedStatusBarPopup;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.runnerAction.BuildTargetAction;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.gutters.ExecutorActionsUtil;
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction;
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BazelFileTargetsWidget extends EditorBasedStatusBarPopup {
    /**
     * Make sure this id matches the one of the extension registration <statusBarWidgetFactory/> in the plugin.xml file
     */
    static final String WIDGET_ID = "BazelFileTargetsWidget";

    // consulo: Consulo's EditorBasedStatusBarPopup takes the widget factory
    public BazelFileTargetsWidget(Project project, StatusBarWidgetFactory factory) {
        super(project, factory, false);
    }

    // consulo: StatusBarWidget.ID() -> getId()
    @Override
    public String getId() {
        return WIDGET_ID;
    }

    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        if (file == null) {
            return inactiveWidgetState(BazelPluginIcons.bazel());
        }
        else {
            return activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file, BazelPluginIcons.bazel());
        }
    }

    private WidgetState activeWidgetStateIfIncludedInAnyTargetOrInactiveState(VirtualFile file, Image icon) {
        List<Label> targets = TargetStorage.getTargetStorage(getProject()).getTargetsForFile(file);
        if (targets.isEmpty()) {
            return inactiveWidgetState(icon);
        }
        else {
            return activeWidgetState(targets.get(0), icon);
        }
    }

    private WidgetState inactiveWidgetState(Image icon) {
        WidgetState state = new WidgetState(BazelPluginLocalize.widgetTooltipTextInactive(), LocalizeValue.empty(), false);
        state.setIcon(icon);

        return state;
    }

    private WidgetState activeWidgetState(@Nullable Label loadedTarget, Image icon) {
        String text = loadedTarget != null ? BazelRepoMappingUtils.toShortString(loadedTarget, getProject()) : "";
        WidgetState state = new WidgetState(BazelPluginLocalize.widgetTooltipTextActive(), LocalizeValue.of(text), true);
        state.setIcon(icon);

        return state;
    }

    @Override
    protected ListPopup createPopup(DataContext context) {
        VirtualFile file = Objects.requireNonNull(context.getData(VirtualFile.KEY));
        ActionGroup group = calculatePopupGroup(file);
        JBPopupFactory.ActionSelectionAid mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS;
        String title = BazelPluginLocalize.widgetTitle().get();

        return JBPopupFactory.getInstance().createActionGroupPopup(title, group, context, mnemonics, true);
    }

    private ActionGroup calculatePopupGroup(VirtualFile file) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(getProject());
        List<Label> targetIds = targetUtils.getTargetsForFile(file);
        Set<Label> targetIdSet = new HashSet<>(targetIds);
        List<Label> executableTargetIds = targetUtils.getExecutableTargetsForFile(file).stream()
            .filter(it -> !targetIdSet.contains(it))
            .toList();

        List<BuildTarget> targets = getTargetInfos(targetIds);
        List<BuildTarget> executableTargets = getTargetInfos(executableTargetIds);

        DefaultActionGroup it = new DefaultActionGroup();
        it.addAll(targets.stream().map(this::calculatePopupGroup).toList());
        if (!targets.isEmpty() && !executableTargets.isEmpty()) {
            it.addSeparator();
        }
        it.addAll(executableTargets.stream().map(this::calculatePopupGroup).toList());
        return it;
    }

    private List<BuildTarget> getTargetInfos(List<Label> receiver) {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(getProject());
        return receiver.stream().map(targetUtils::getTargetSummary).filter(Objects::nonNull).map(Objects::requireNonNull).toList();
    }

    private ActionGroup calculatePopupGroup(BuildTarget receiver) {
        Label id = BuildTarget.getId(receiver);
        DefaultActionGroup it = createActionGroup(BazelRepoMappingUtils.toShortString(id, getProject()));
        it.add(new CopyTargetIdAction.FromTargetInfo(receiver));
        it.addSeparator();
        it.add(new BuildTargetAction(id));
        it.addAll(ExecutorActionsUtil.getExecutorActions(getProject(), receiver));
        it.addSeparator();
        it.add(new BazelJumpToBuildFileAction.NonXmlRegistered(() -> receiver));
        it.add(new StarlarkDebugAction(id));
        return it;
    }

    private DefaultActionGroup createActionGroup(String name) {
        DefaultActionGroup group = new DefaultActionGroup(LocalizeValue.of(name), true);
        group.getTemplatePresentation().setText(name, false); // BAZEL-3104 - do not parse label as mnemonic text
        return group;
    }

    @Override
    protected StatusBarWidget createInstance(Project project) {
        return new BazelFileTargetsWidget(project, myFactory);
    }

    public static CompletableFuture<Void> updateBazelFileTargetsWidget(Project project) {
        if (!Application.get().isHeadlessEnvironment()) {
            StatusBarWidgetsManager statusBarWidgetsManager = StatusBarWidgetsManager.getInstance(project);
            // consulo: Consulo's StatusBarWidgetsManager.updateWidget takes the UI access
            statusBarWidgetsManager.updateWidget(BazelFileTargetsWidgetFactory.class, project.getUIAccess());
        }
        return CompletableFuture.completedFuture(null);
    }
}

@ExtensionImpl(id = BazelFileTargetsWidget.WIDGET_ID)
class BazelFileTargetsWidgetFactory implements StatusBarWidgetFactory {
    public BazelFileTargetsWidgetFactory() {
    }

    @Override
    public String getId() {
        return BazelFileTargetsWidget.WIDGET_ID;
    }

    @Override
    public String getDisplayName() {
        return BazelPluginLocalize.widgetFactoryDisplayName().get();
    }

    @Override
    public boolean isAvailable(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    @Override
    public StatusBarWidget createWidget(Project project) {
        return new BazelFileTargetsWidget(project, this);
    }

    @Override
    public void disposeWidget(StatusBarWidget widget) {
        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return true;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
