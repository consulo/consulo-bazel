package org.jetbrains.bazel.flow.open;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.ProjectPreSyncHook;
import org.jetbrains.bazel.sync.ProjectSyncAbortException;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtensionImpl(order = "first")
public class ValidateProjectViewPreSyncHook implements ProjectPreSyncHook {
    @Override
    public CompletableFuture<Void> onPreSync(ProjectPreSyncHook.ProjectPreSyncHookEnvironment environment) {
        Project project = environment.project();
        return ProjectPreSyncHook.withSubtask(environment, BazelPluginLocalize.projectViewValidateSubtask(), taskId -> {
            // consulo: upstream `var hasErrors`, written from the callback
            AtomicBoolean hasErrors = new AtomicBoolean(false);
            return ProjectViewService
                .getInstance(project)
                .forceReparseCurrentProjectViewFiles(problem -> {
                    @Nullable LineColumn start = start(problem);
                    util.getSyncConsole(project).addDiagnosticMessage(
                        /* taskId = */ taskId,
                        /* path = */ problem.sourcePath(),
                        /* line = */ start != null ? start.line() : -1,
                        /* column = */ start != null ? start.column() : -1,
                        /* message = */ problem.message(),
                        /* description = */ null,
                        /* severity = */ toMessageKind(problem.severity())
                    );
                    if (problem.severity() == ProjectViewProblem.Severity.Error) {
                        hasErrors.set(true);
                    }
                })
                .thenApply(unused -> {
                    if (hasErrors.get()) {
                        throw new ProjectSyncAbortException();
                    }
                    return null;
                });
        });
    }

    private static @Nullable LineColumn start(ProjectViewProblem receiver) {
        PsiElement element = receiver.element();
        PsiFile containingFile = element != null ? element.getContainingFile() : null;
        if (element == null || containingFile == null) {
            return null;
        }
        return offsetToLineColumn(
            containingFile.getViewProvider().getContents(),
            element.getTextRange().getStartOffset()
        );
    }

    private static MessageEvent.Kind toMessageKind(ProjectViewProblem.Severity receiver) {
        return switch (receiver) {
            case Error -> MessageEvent.Kind.ERROR;
            case Warning, WeakWarning -> MessageEvent.Kind.WARNING;
        };
    }

    // consulo: IntelliJ StringUtil.offsetToLineColumn and LineColumn are missing in Consulo
    private static @Nullable LineColumn offsetToLineColumn(CharSequence text, int offset) {
        int line = StringUtil.offsetToLineNumber(text, offset);
        if (line < 0) {
            return null;
        }
        return new LineColumn(line, offset - StringUtil.lineColToOffset(text, line, 0));
    }

    private record LineColumn(int line, int column) {
    }
}
