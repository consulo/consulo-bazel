package org.jetbrains.bazel.ui.widgets.tool.window.utils;

import consulo.application.ReadAction;
import consulo.dataContext.DataContext;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.PopupHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction;
import org.jetbrains.bazel.runnerAction.BuildTargetAction;
import org.jetbrains.bazel.sync.action.ResyncFilesAction;
import org.jetbrains.bazel.ui.gutters.BazelRunLocation;
import org.jetbrains.bazel.ui.gutters.ExecutorActionsUtil;
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction;
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

public abstract class LoadedTargetsMouseListener extends PopupHandler {
    // consulo: ActionPlaces.TOOLWINDOW_POPUP does not exist in Consulo's ActionPlaces, same value
    private static final String TOOLWINDOW_POPUP = "ToolwindowPopup";

    private final Project myProject;

    public LoadedTargetsMouseListener(Project project) {
        myProject = project;
    }

    public abstract boolean isPointSelectable(Point point);

    public abstract @Nullable BuildTarget getSelectedBuildTarget();

    public abstract @Nullable VirtualFile getSelectedDirectory();

    public abstract CopyTargetIdAction getCopyTargetIdAction();

    public abstract BazelJumpToBuildFileAction getBazelJumpToBuildFileAction();

    public abstract String getSelectedComponentName();

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (isDoubleClick(mouseEvent) && isPointSelectable(mouseEvent.getPoint())) {
            BuildTarget target = getSelectedBuildTarget();
            if (target == null) {
                return;
            }
            runOrBuildTarget(myProject, target);
        }
        else {
            super.mouseClicked(mouseEvent);
        }
    }

    /**
     * Inherit from PopupHandler instead of MouseListener to be called in
     * <a href="https://code.jetbrains.team/p/ij/repositories/ultimate/files/ebcc1e5735999c995ba1dd00be8003b66d2e8309/remote-dev/rd-ui/src/com/jetbrains/rd/ui/bedsl/BeDslBehavior.kt?tab=source&line=98&lines-count=1">remote dev scenarios</a>
     */
    @Override
    public void invokePopup(
        Component component,
        int x,
        int y
    ) {
        showPopup(component, x, y);
    }

    private void showPopup(
        Component component,
        int x,
        int y
    ) {
        BuildTarget selectedBuildTarget = getSelectedBuildTarget();
        ActionGroup actionGroup;
        if (selectedBuildTarget != null) {
            actionGroup = calculatePopupGroup(selectedBuildTarget);
        }
        else {
            VirtualFile selectedDirectory = getSelectedDirectory();
            if (selectedDirectory == null) {
                return;
            }
            actionGroup = calculatePopupGroup(selectedDirectory);
        }

        ActionManager
            .getInstance()
            .createActionPopupMenu(TOOLWINDOW_POPUP, actionGroup)
            .getComponent()
            .show(component, x, y);
    }

    private ActionGroup calculatePopupGroup(BuildTarget target) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.addAction(getCopyTargetIdAction());
        group.addSeparator();
        group.addAction(new BuildTargetAction(BuildTarget.getId(target)));
        // We're on EDT so use runReadActionBlocking
        group.addAll(ReadAction.<List<AnAction>, RuntimeException>compute(() -> ExecutorActionsUtil.getExecutorActions(myProject, target)));
        group.addAction(getBazelJumpToBuildFileAction());
        group.add(new StarlarkDebugAction(BuildTarget.getId(target)));
        return group;
    }

    private ActionGroup calculatePopupGroup(VirtualFile directory) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (ResyncFilesAction.isResyncable(directory)) {
            group.addAction(new ResyncFilesAction.NonXmlRegistered(List.of(directory)));
            group.addSeparator();
        }
        group.addAll(
            ReadAction.<List<AnAction>, RuntimeException>compute(() -> {
                PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(directory);
                return ExecutorActionsUtil.getExecutorActions(new PsiLocation<>(Objects.requireNonNull(psiDirectory)));
            })
        );
        return group;
    }

    // consulo: IntelliJ's MouseEvent.mouseButton (codeInsight.hints.presentation) is not API, MouseButton.Left is SwingUtilities.isLeftMouseButton
    private static boolean isDoubleClick(MouseEvent receiver) {
        return SwingUtilities.isLeftMouseButton(receiver) && receiver.getClickCount() == 2;
    }

    @RequiredUIAccess
    public static void runOrBuildTarget(Project project, BuildTarget target) {
        RuleType ruleType = target.getKind().ruleType();
        if (ruleType == RuleType.TEST || ruleType == RuleType.BINARY) {
            BazelRunLocation location = new BazelRunLocation(project, target);
            DataContext dataContext = DataContext.builder()
                .add(Project.KEY, project)
                .add(Location.DATA_KEY, location)
                .build();
            // consulo: Consulo's ConfigurationContext.getFromContext requires the read action and has no place parameter
            ConfigurationContext configurationContext =
                ReadAction.<ConfigurationContext, RuntimeException>compute(() -> ConfigurationContext.getFromContext(dataContext));
            // consulo: PreferredProducerFind.createConfiguration(location, context) is internal; ConfigurationContext.getConfiguration()
            // calls it for the context location
            RunnerAndConfigurationSettings settings = ReadAction.<@Nullable RunnerAndConfigurationSettings, RuntimeException>compute(
                configurationContext::getConfiguration
            );
            if (settings == null) {
                return;
            }

            // consulo: RunManagerEx.setTemporaryConfiguration is internal; the same steps through RunManager
            // (without trimming the oldest temporary configurations)
            RunManager runManager = RunManager.getInstance(project);
            settings.setTemporary(true);
            runManager.addConfiguration(settings, false);
            runManager.setSelectedConfiguration(settings);
            Executor executor = DefaultRunExecutor.getRunExecutorInstance();
            ProgramRunner<?> runner = RunnerRegistry.getInstance().getRunner(executor.getId(), settings.getConfiguration());
            if (runner == null) {
                return;
            }
            ExecutionEnvironment executionEnvironment =
                new ExecutionEnvironmentBuilder(project, executor)
                    .runnerAndSettings(runner, settings)
                    .build();
            try {
                runner.execute(executionEnvironment);
            }
            catch (ExecutionException e) {
                // consulo: checked in Java, upstream (Kotlin) lets it propagate
                throw new RuntimeException(e);
            }
        }
        else {
            BuildTargetAction.buildTarget(project, BuildTarget.getId(target));
        }
    }
}
