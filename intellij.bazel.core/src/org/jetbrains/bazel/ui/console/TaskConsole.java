package org.jetbrains.bazel.ui.console;

import consulo.application.Application;
import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.Failure;
import consulo.build.ui.event.FailureResult;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.event.StartBuildEvent;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.progress.ConsoleService;
import org.jetbrains.bazel.progress.ShowConsole;
import org.jetbrains.bazel.sync.status.SyncStatusListener;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.google.common.base.Throwables.getStackTraceAsString;

// consulo: upstream file-level declarations of TaskConsole.kt
public final class TaskConsole {
    private TaskConsole() {
    }

    public abstract static class BaseTaskConsole implements org.jetbrains.bazel.progress.TaskConsole {
        private static final Logger LOG = Logger.getInstance(BaseTaskConsole.class);

        private final BuildProgressListener myTaskView;
        private final String myBasePath;
        private final Project myProject;
        private final boolean myUsePty;

        protected final Set<TaskId> myTasksInProgress = new LinkedHashSet<>();
        private final Map<TaskId, Set<Throwable>> myTaskFailures = new LinkedHashMap<>();
        private final Map<TaskId, TaskId> mySubtaskParentMap = new LinkedHashMap<>();
        private final Map<TaskId, LocalizeValue> mySubtaskMessageMap = new LinkedHashMap<>();

        public BaseTaskConsole(
            BuildProgressListener taskView,
            String basePath,
            Project project,
            boolean usePty
        ) {
            myTaskView = taskView;
            myBasePath = basePath;
            myProject = project;
            myUsePty = usePty;
        }

        // consulo: upstream default argument usePty = BazelFeatureFlags.usePty
        public BaseTaskConsole(
            BuildProgressListener taskView,
            String basePath,
            Project project
        ) {
            this(taskView, basePath, project, BazelFeatureFlags.getUsePty());
        }

        private static BuildEventFactory buildEventFactory() {
            return Application.get().getInstance(BuildEventFactory.class);
        }

        @Override
        public synchronized void startTask(
            TaskId taskId,
            LocalizeValue title,
            LocalizeValue message,
            Runnable cancelAction,
            @Nullable Supplier<? extends CompletableFuture<?>> redoAction,
            ShowConsole showConsole
        ) {
            if (myTasksInProgress.add(taskId)) {
                doStartTask(
                    taskId,
                    BazelPluginLocalize.consoleTasksTitle(BazelPluginConstants.BAZEL_DISPLAY_NAME, title),
                    message,
                    cancelAction,
                    redoAction,
                    showConsole
                );
            }
        }

        private void doStartTask(
            TaskId taskId,
            LocalizeValue title,
            LocalizeValue message,
            Runnable cancelAction,
            @Nullable Supplier<? extends CompletableFuture<?>> redoAction,
            ShowConsole showConsole
        ) {
            // consulo: Consulo DefaultBuildDescriptor has no group id (upstream passes taskId.taskGroupId)
            DefaultBuildDescriptor taskDescriptor = new DefaultBuildDescriptor(taskId, title, myBasePath, System.currentTimeMillis());
            switch (showConsole) {
                case ALWAYS -> {
                    taskDescriptor.setActivateToolWindowWhenAdded(true);
                    taskDescriptor.setActivateToolWindowWhenFailed(true);
                }

                case ON_FAIL -> {
                    taskDescriptor.setActivateToolWindowWhenAdded(false);
                    taskDescriptor.setActivateToolWindowWhenFailed(true);
                }

                case NEVER -> {
                    taskDescriptor.setActivateToolWindowWhenAdded(false);
                    taskDescriptor.setActivateToolWindowWhenFailed(false);
                }
            }
            addRedoActionToDescriptor(taskDescriptor, redoAction);
            addCancelActionToDescriptor(taskId, taskDescriptor, cancelAction);
            if (myUsePty) {
                addPtyToDescriptor(taskId, taskDescriptor);
            }
            StartBuildEvent startEvent = buildEventFactory().createStartBuildEvent(taskDescriptor, message);
            myTaskView.onEvent(taskId, startEvent);
        }

        private void addCancelActionToDescriptor(
            TaskId taskId,
            DefaultBuildDescriptor taskDescriptor,
            Runnable doCancelAction
        ) {
            CancelAction cancelAction = new CancelAction(doCancelAction, taskId);
            taskDescriptor.withAction(cancelAction);
        }

        private void addRedoActionToDescriptor(
            DefaultBuildDescriptor taskDescriptor,
            @Nullable Supplier<? extends CompletableFuture<?>> redoAction
        ) {
            AnAction action = calculateRedoAction(redoAction);
            taskDescriptor.withAction(action);
        }

        private void addPtyToDescriptor(TaskId taskId, DefaultBuildDescriptor taskDescriptor) {
            // TODO consulo: TerminalExecutionConsoleBuilder (terminal plugin), BuildContentDescriptor and jediterm TtyConnector
            //  are missing, the build view keeps its default (non-pty) console
            //val consoleDelegate = log.runAndLogException {
            //  TerminalExecutionConsoleBuilder(project).build()
            //} ?: return
            //
            //val console = object : ConsoleView by consoleDelegate, BuildProgressListener {
            //  override fun onEvent(buildId: Any, event: BuildEvent) {
            //    if (event is OutputBuildEvent) {
            //      consoleDelegate.print(event.message, ConsoleViewContentType.getConsoleViewType(event.outputType))
            //    }
            //  }
            //}
            //
            //val contentDescriptor =
            //  BuildContentDescriptor(console, null, consoleDelegate.component, null)
            //contentDescriptor.apply {
            //  // copy values from taskDescriptor, where it had been set before already
            //  isActivateToolWindowWhenAdded = taskDescriptor.isActivateToolWindowWhenAdded
            //  isActivateToolWindowWhenFailed = taskDescriptor.isActivateToolWindowWhenFailed
            //}
            //Disposer.register(contentDescriptor, consoleDelegate)
            //Disposer.register(taskView as Disposable, contentDescriptor)
            //
            //// TerminalExecutionConsoleBuilder expects a ProcessHandler, so we provide a stub because we handle process interaction
            //// in the server.
            //// See https://github.com/bazelbuild/intellij/pull/7001/files
            //val ttyConnector = object : TtyConnector, Disposable {
            //  init {
            //    Disposer.register(contentDescriptor, this)
            //  }
            //
            //  private var connected: Boolean = true
            //  override fun dispose() {
            //    connected = false
            //  }
            //
            //  override fun read(buf: CharArray?, offset: Int, length: Int): Int = 0
            //  override fun write(bytes: ByteArray?) = Unit
            //  override fun write(string: String?) = Unit
            //  override fun isConnected(): Boolean = connected
            //  override fun waitFor(): Int = 0
            //  override fun ready(): Boolean = true
            //  override fun getName(): String = ""
            //  override fun close() = Unit
            //  override fun resize(termSize: TermSize) = Unit
            //}
            //
            //consoleDelegate.terminalWidget.createTerminalSession(ttyConnector)
            //consoleDelegate.terminalWidget.start()
            //taskDescriptor.withContentDescriptor { contentDescriptor }
        }

        protected abstract AnAction calculateRedoAction(@Nullable Supplier<? extends CompletableFuture<?>> redoAction);

        @Override
        public boolean hasTasksInProgress() {
            return !myTasksInProgress.isEmpty();
        }

        @Override
        public synchronized void finishTask(
            TaskId taskId,
            LocalizeValue message,
            EventResult result
        ) {
            if (myTasksInProgress.contains(taskId)) {
                doFinishTask(taskId, message, result);
            }
        }

        private void doFinishTask(
            TaskId taskId,
            LocalizeValue message,
            EventResult result
        ) {
            EventResult resultWithoutShownErrors = withoutShownError(taskId, result);
            if (resultWithoutShownErrors instanceof FailureResult failureResult) {
                for (Failure it : failureResult.getFailures()) {
                    LOG.warn(it.getMessage().get(), it.getError());
                    Throwable error = it.getError();
                    if (error != null) {
                        addMessage(taskId, getStackTraceAsString(error));
                    }
                }
            }
            finishChildrenSubtasks(taskId, result);
            myTasksInProgress.remove(taskId);
            myTaskFailures.remove(taskId);
            mySubtaskParentMap.entrySet().removeIf(it -> Objects.equals(findActiveRootTaskId(it.getValue()), taskId));
            BuildEvent event =
                buildEventFactory().createFinishBuildEvent(taskId, null, System.currentTimeMillis(), message, resultWithoutShownErrors);
            myTaskView.onEvent(taskId, event);
        }

        @Override
        public synchronized void startSubtask(
            TaskId subtaskId,
            LocalizeValue message
        ) {
            TaskId rootTaskId = findActiveRootTaskId(subtaskId);
            if (rootTaskId == null) {
                return;
            }
            TaskId activeTaskId = findActiveTaskId(subtaskId);
            if (activeTaskId == null) {
                return;
            }
            if (activeTaskId.equals(subtaskId)) {
                throw new IllegalStateException("Cannot restart task " + subtaskId);
            }
            doStartSubtask(rootTaskId, activeTaskId, subtaskId, message);
        }

        private void doStartSubtask(
            TaskId rootTaskId,
            TaskId parentTaskId,
            TaskId subtaskId,
            LocalizeValue message
        ) {
            mySubtaskMessageMap.put(subtaskId, message);
            mySubtaskParentMap.put(subtaskId, parentTaskId);
            // consulo: upstream sends ProgressBuildEventImpl(subtaskId, parentTaskId, time, message, -1, -1, ""),
            // BuildEventFactory has no progress event, a start event creates the same subtask node
            BuildEvent event = buildEventFactory().createStartEvent(subtaskId, parentTaskId, System.currentTimeMillis(), message);
            myTaskView.onEvent(rootTaskId, event);
        }

        @Override
        public synchronized void finishSubtask(
            TaskId subtaskId,
            @Nullable LocalizeValue message,
            EventResult result
        ) {
            TaskId parentTaskId = mySubtaskParentMap.get(subtaskId);
            if (parentTaskId == null) {
                return;
            }
            TaskId rootTaskId = findActiveRootTaskId(parentTaskId);
            if (rootTaskId == null) {
                return;
            }
            doFinishSubtask(rootTaskId, subtaskId, message, result);
        }

        private void doFinishSubtask(
            TaskId rootTask,
            TaskId subtaskId,
            @Nullable LocalizeValue message,
            EventResult result
        ) {
            LocalizeValue removedMessage = mySubtaskMessageMap.remove(subtaskId);
            LocalizeValue savedMessage = removedMessage != null ? removedMessage : LocalizeValue.empty();
            LocalizeValue messageToDisplay = message != null ? message : savedMessage;

            EventResult resultWithoutShownErrors = withoutShownError(rootTask, result);
            finishChildrenSubtasks(subtaskId, result);
            BuildEvent event = buildEventFactory().createFinishEvent(
                subtaskId,
                null,
                System.currentTimeMillis(),
                messageToDisplay,
                resultWithoutShownErrors
            );
            myTaskView.onEvent(rootTask, event);

            mySubtaskParentMap.remove(subtaskId);
        }

        /**
         * Shows a plain failure when the root task already shows the exception of {@code result}.
         */
        private EventResult withoutShownError(TaskId rootTaskId, EventResult result) {
            if (!(result instanceof FailureResult failureResult)) {
                return result;
            }
            List<? extends Failure> failures = failureResult.getFailures();
            if (failures.size() != 1) {
                return result;
            }
            Throwable error = failures.get(0).getError();
            if (error == null) {
                return result;
            }
            return registerFailure(rootTaskId, error) ? result : buildEventFactory().newFailure().createResult();
        }

        /**
         * Returns false when the root task already shows {@code error} or one of its causes.
         * The check includes the causes, because kotlinx.coroutines can rethrow a copy of an exception with the original as the cause.
         */
        private boolean registerFailure(TaskId rootTaskId, Throwable error) {
            Set<Throwable> failures = myTaskFailures.computeIfAbsent(rootTaskId, it -> new LinkedHashSet<>());
            // consulo: upstream `error.causeChain()` (com.intellij.util), the error followed by its causes
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (failures.contains(cause)) {
                    return false;
                }
            }
            return failures.add(error);
        }

        private void finishChildrenSubtasks(TaskId parentId, EventResult result) {
            List<TaskId> children = new ArrayList<>();
            for (Map.Entry<TaskId, TaskId> entry : mySubtaskParentMap.entrySet()) {
                if (entry.getValue().equals(parentId)) {
                    children.add(entry.getKey());
                }
            }
            for (TaskId it : children) {
                finishSubtask(it, null, result);
            }
        }

        @Override
        public synchronized void addDiagnosticMessage(
            TaskId taskId,
            @Nullable Path path,
            int line,
            int column,
            LocalizeValue message,
            @Nullable String description,
            MessageEvent.Kind severity,
            @Nullable Throwable error
        ) {
            if (message.get().isBlank()) {
                return;
            }
            TaskId rootTaskId = findActiveRootTaskId(taskId);
            if (rootTaskId == null) {
                return;
            }
            TaskId activeTaskId = findActiveTaskId(taskId);
            if (activeTaskId == null) {
                return;
            }
            if (error != null && !registerFailure(rootTaskId, error)) {
                return;
            }
            FilePosition filePosition = path != null ? new FilePosition(path.toFile(), line, column) : null;
            doAddDiagnosticMessage(rootTaskId, activeTaskId, filePosition, message, description, severity);
        }

        private void doAddDiagnosticMessage(
            TaskId taskId,
            TaskId subtaskId,
            @Nullable FilePosition filePosition,
            LocalizeValue message,
            @Nullable String description,
            MessageEvent.Kind severity
        ) {
            // consulo: Consulo message events need a notification group (upstream passes a null group)
            BuildEvent event;
            if (filePosition == null) {
                event = buildEventFactory().createMessageEvent(
                    subtaskId,
                    severity,
                    BazelNotificationGroupContributor.BAZEL,
                    message.map(this::prepareTextToPrint),
                    description != null ? LocalizeValue.of(description) : message.map(this::prepareTextToPrint)
                );
            }
            else {
                event = buildEventFactory().createFileMessageEvent(
                    subtaskId,
                    severity,
                    BazelNotificationGroupContributor.BAZEL,
                    message.map(this::prepareTextToPrint),
                    LocalizeValue.ofNullable(description),
                    filePosition
                );
            }

            myTaskView.onEvent(taskId, event);
        }

        @Override
        public synchronized void addMessage(TaskId taskId, String message) {
            if (message.isBlank()) {
                return;
            }
            TaskId rootTaskId = findActiveRootTaskId(taskId);
            if (rootTaskId == null) {
                return;
            }
            TaskId activeTaskId = findActiveTaskId(taskId);
            if (activeTaskId == null) {
                return;
            }
            doAddMessage(activeTaskId, message);
        }

        private void doAddMessage(TaskId taskId, String message) {
            if (myTasksInProgress.contains(taskId)) {
                sendMessageEvent(taskId, message);
            }
            else if (mySubtaskParentMap.containsKey(taskId)) {
                sendMessageEvent(taskId, message);
                sendMessageToAncestors(taskId, message);
            }
        }

        private void sendMessageToAncestors(TaskId taskId, String message) {
            TaskId parentTask = mySubtaskParentMap.get(taskId);
            if (parentTask != null) {
                sendMessageEvent(parentTask, message);
                sendMessageToAncestors(parentTask, message);
            }
        }

        private void sendMessageEvent(TaskId taskId, String message) {
            TaskId subtaskId = myTasksInProgress.contains(taskId) ? null : taskId;
            BuildEvent event = buildEventFactory().createOutputBuildEvent(subtaskId, message, true);
            TaskId rootTaskId = findActiveRootTaskId(taskId);
            if (rootTaskId != null) {
                myTaskView.onEvent(rootTaskId, event);
            }
        }

        private @Nullable TaskId findActiveTaskId(TaskId taskId) {
            TaskId id = taskId;
            while (id != null) {
                if (mySubtaskParentMap.containsKey(id) || myTasksInProgress.contains(id)) {
                    return id;
                }
                id = id.parent();
            }
            return null;
        }

        private @Nullable TaskId findActiveRootTaskId(TaskId taskId) {
            TaskId id = taskId;
            while (id != null) {
                if (myTasksInProgress.contains(id)) {
                    return id;
                }
                id = id.parent();
            }
            return null;
        }

        private String prepareTextToPrint(String text) {
            return text.endsWith("\n") ? text : text + "\n";
        }

        private class CancelAction extends DumbAwareAction implements AnActionWithSyncUpdate {
            private final Runnable myDoCancelAction;
            private final TaskId myTaskId;

            private volatile boolean myCancelActionActivated = false;

            CancelAction(Runnable doCancelAction, TaskId taskId) {
                super(BazelPluginLocalize.cancelActionText(), LocalizeValue.empty(), PlatformIconGroup.actionsSuspend());
                myDoCancelAction = doCancelAction;
                myTaskId = taskId;

                myProject.getMessageBus().connect().subscribe(
                    SyncStatusListener.class,
                    new SyncStatusListener() {
                        @Override
                        public void syncStarted() {
                        }

                        @Override
                        public void syncFinished(boolean canceled) {
                        }
                    }
                );
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                myCancelActionActivated = true;
                myDoCancelAction.run();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(!myCancelActionActivated && myTasksInProgress.contains(myTaskId));
            }

            // consulo: no getActionUpdateThread() (upstream: ActionUpdateThread.BGT)
        }
    }

    public static boolean isBuildInProgress(Project receiver) {
        return ConsoleService.getInstance(receiver).getBuildConsole().hasTasksInProgress();
    }
}

class SyncTaskConsole extends TaskConsole.BaseTaskConsole {
    SyncTaskConsole(
        BuildProgressListener taskView,
        String basePath,
        Project project
    ) {
        super(taskView, basePath, project);
    }

    @Override
    protected AnAction calculateRedoAction(@Nullable Supplier<? extends CompletableFuture<?>> redoAction) {
        return new SuspendableAction(BazelPluginLocalize.resyncActionText(), PlatformIconGroup.actionsRefresh()) {
            private volatile boolean myRedoActionActivated = false;

            @Override
            protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
                myRedoActionActivated = true;
                if (redoAction != null) {
                    return redoAction.get();
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            protected void update(Project project, AnActionEvent e) {
                e.getPresentation().setEnabled(
                    !myRedoActionActivated && redoAction != null && !SyncStatusService.isSyncInProgress(project) &&
                        !TaskConsole.isBuildInProgress(project)
                );
            }
        };
    }
}

class BuildTaskConsole extends TaskConsole.BaseTaskConsole {
    BuildTaskConsole(
        BuildProgressListener taskView,
        String basePath,
        Project project
    ) {
        super(taskView, basePath, project);
    }

    @Override
    protected AnAction calculateRedoAction(@Nullable Supplier<? extends CompletableFuture<?>> redoAction) {
        return new SuspendableAction(BazelPluginLocalize.rebuildActionText(), PlatformIconGroup.actionsCompile()) {
            private volatile boolean myRedoActionActivated = false;

            @Override
            protected CompletableFuture<?> actionPerformed(Project project, AnActionEvent e) {
                myRedoActionActivated = true;
                if (redoAction != null) {
                    return redoAction.get();
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            protected void update(Project project, AnActionEvent e) {
                e.getPresentation().setEnabled(!myRedoActionActivated && redoAction != null && myTasksInProgress.isEmpty());
            }

            // consulo: no getActionUpdateThread() (upstream: ActionUpdateThread.BGT)
        };
    }
}
