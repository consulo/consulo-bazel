package org.jetbrains.bazel.taskEvents;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.event.MessageEvent.Kind;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.CachedTestLog;
import org.jetbrains.bsp.protocol.CoverageReport;
import org.jetbrains.bsp.protocol.DiagnosticSeverity;
import org.jetbrains.bsp.protocol.LogMessageParams;
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams;
import org.jetbrains.bsp.protocol.TaskFinishParams;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskId.TaskGroupId;
import org.jetbrains.bsp.protocol.TaskStartParams;
import org.jetbrains.bsp.protocol.TextDocumentIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelTaskEventsService implements BazelTaskEventsHandler {
    private final Project myProject;

    private final ConcurrentHashMap<TaskGroupId, BazelTaskListener> myTaskListeners = new ConcurrentHashMap<>();

    @Inject
    public BazelTaskEventsService(Project project) {
        myProject = project;
    }

    public void saveListener(TaskGroupId id, BazelTaskListener listener) {
        if (id.equals(TaskGroupId.EMPTY)) {
            throw new IllegalStateException("Attempt to register TaskGroupId.EMPTY listener");
        }
        if (myTaskListeners.putIfAbsent(id, listener) != null) {
            throw new IllegalStateException("Listener for task " + id + " exists already");
        }
    }

    public void withListener(TaskId id, Consumer<BazelTaskListener> block) {
        // `TaskGroupId.EMPTY` is explicitly created to silence given steps
        if (id.taskGroupId().equals(TaskGroupId.EMPTY)) {
            return;
        }

        @Nullable BazelTaskListener listener = myTaskListeners.get(id.taskGroupId());
        if (listener == null) {
            LOG.warn("No task listener found for task " + id);
            return;
        }

        block.accept(listener);
    }

    public void removeListener(TaskGroupId id) {
        myTaskListeners.remove(id);
    }

    @Override
    public void onBuildTaskStart(TaskStartParams params) {
        TaskId taskId = params.getTaskId();
        @Nullable String message = params.getMessage();
        if (message == null) {
            return;
        }
        withListener(taskId, it -> {
            it.onTaskStart(taskId, message, params.getData());
        });
    }

    @Override
    public void onBuildTaskFinish(TaskFinishParams params) {
        TaskId taskId = params.taskId();
        BazelStatus status = params.status();
        @Nullable String message = params.message();
        if (message == null) {
            return;
        }

        withListener(taskId, it -> {
            it.onTaskFinish(taskId, message, status, params.data());
        });
    }

    @Override
    public void onBuildLogMessage(LogMessageParams params) {
        TaskId originId = params.task();
        String message = params.message();
        // Drop the empty-target-set warning emitted by the 0-target "probe" build the sync runs to read options.
        if (isEmptyTargetSetWarning(message)) {
            return;
        }

        withListener(originId, it -> {
            it.onLogMessage(originId, message);
        });
    }

    @Override
    public void onBuildPublishDiagnostics(PublishDiagnosticsParams params) {
        withListener(params.taskId(), it -> {
            params.diagnostics().forEach(diag -> {
                @Nullable TextDocumentIdentifier textDocument = params.textDocument();
                @Nullable DiagnosticSeverity level = diag.level();
                Kind severity;
                if (level == null) {
                    severity = Kind.SIMPLE;
                }
                else {
                    severity = switch (level) {
                        case ERROR -> Kind.ERROR;
                        case WARNING -> Kind.WARNING;
                        case INFORMATION -> Kind.INFO;
                        case HINT -> Kind.INFO;
                    };
                }
                it.onDiagnostic(
                    params.taskId(),
                    textDocument != null ? textDocument.path() : null,
                    params.buildTarget(),
                    diag.position().line(),
                    diag.position().character(),
                    severity,
                    diag.message()
                );
            });
        });
    }

    @Override
    public void onPublishCoverageReport(CoverageReport report) {
        withListener(report.taskId(), it -> {
            it.onPublishCoverageReport(report.coverageReport());
        });
    }

    @Override
    public void onCachedTestLog(CachedTestLog testLog) {
        withListener(testLog.taskId(), it -> {
            it.onCachedTestLog(testLog.testLog());
        });
    }

    @Override
    public void onBazelInvocationMetrics(BazelInvocationMetrics metrics) {
        // consulo: usage statistics are not ported
        // BazelBuildMetricsCollector.log(project, metrics)
    }

    private static final Logger LOG = Logger.getInstance(BazelTaskEventsService.class);

    public static BazelTaskEventsService getInstance(Project project) {
        return project.getInstance(BazelTaskEventsService.class);
    }

    /**
     * Recognizes Bazel's warning for an invocation with an empty target set, emitted line by line.
     * <p>
     * The sync runs a 0-target "probe" build (to read the resolved options before the aspect build), which always
     * triggers this warning. A real user build has targets and won't produce it, so dropping it globally is safe.
     * Matched on stable, non-localized fragments observed on Bazel 9; see https://github.com/bazelbuild/bazel/issues/6811.
     */
    // consulo: upstream top-level function of BazelTaskEventsService.kt
    public static boolean isEmptyTargetSetWarning(String line) {
        String lowercased = line.toLowerCase(Locale.ROOT);
        return lowercased.contains("usage: bazel build") ||
            lowercased.contains("invoke `bazel help build`") ||
            lowercased.contains("empty set of targets");
    }
}
