package org.jetbrains.bazel.clion.run.debug;

import consulo.execution.RunCanceledByUserException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.clion.run.debug.checkDebugInfo.CcDebugInfoCheck;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

// consulo: top-level declarations of upstream resolveDebugInfoIssues.kt
public final class resolveDebugInfoIssues {
    private static final String DEFAULT_PROJECT_VIEW_NAME = ".projectview";

    private resolveDebugInfoIssues() {
    }

    // consulo: upstream internal top-level enum of resolveDebugInfoIssues.kt
    public enum CcDebugInfoDecision {
        CONTINUE,
        RETRY_WITH_DBG
    }

    /**
     * Asks the user how to continue after {@link checkDebugInfo#checkCcDebugInfo} found issues. Does not ask if the user
     * dismissed the dialog for the project. Throws {@link RunCanceledByUserException} if the user aborts
     * the debug session.
     *
     * @param result     null if the check did not run
     * @param allowRetry false hides the option to retry with {@code --compilation_mode=dbg}
     */
    // consulo: @Throws(ExecutionException::class) suspend fun -> the future fails with the ExecutionException
    static CompletableFuture<CcDebugInfoDecision> resolveDebugInfoIssues(
        Project project,
        Label label,
        CcDebugInfoCheck.@Nullable Result result,
        boolean allowRetry
    ) {
        if (result == null || result.issues().isEmpty() || CcDebugInfoPrompt.isCcDebugInfoDialogDismissed(project)) {
            return CompletableFuture.completedFuture(CcDebugInfoDecision.CONTINUE);
        }

        // the quick fix writes to the project view file, thus it needs the file
        VirtualFile projectViewFile = projectView.findProjectViewFile(project);
        boolean canUseDbg = allowRetry && projectViewFile != null && result.isFixableByCompilationMode();

        String projectViewFileName = projectViewFile == null ? DEFAULT_PROJECT_VIEW_NAME : projectViewFile.getName();
        return CcDebugInfoPrompt.askCcDebugInfoChoice(project, label, result, projectViewFileName, canUseDbg).thenCompose(choice ->
            switch (choice) {
                case DEBUG_ANYWAY -> CompletableFuture.completedFuture(CcDebugInfoDecision.CONTINUE);
                case ALWAYS_DEBUG -> {
                    CcDebugInfoPrompt.dismissCcDebugInfoDialog(project);
                    yield CompletableFuture.completedFuture(CcDebugInfoDecision.CONTINUE);
                }
                case USE_DBG -> CompletableFuture.completedFuture(CcDebugInfoDecision.RETRY_WITH_DBG);
                case OPEN_PROJECT_VIEW -> {
                    CompletableFuture<?> opened = projectViewFile != null
                        ? projectView.openProjectViewAtBuildFlags(project, projectViewFile)
                        : CompletableFuture.completedFuture(null);
                    yield opened.<CcDebugInfoDecision>thenApply(ignored -> {
                        throw new CompletionException(new RunCanceledByUserException());
                    });
                }
                case CANCEL -> CompletableFuture.<CcDebugInfoDecision>failedFuture(new RunCanceledByUserException());
            }
        );
    }
}
