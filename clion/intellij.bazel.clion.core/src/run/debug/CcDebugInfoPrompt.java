package org.jetbrains.bazel.clion.run.debug;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoPromptService.CcDebugInfoChoice;
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoPromptService.CcDebugInfoRequest;
import org.jetbrains.bazel.clion.run.debug.checkDebugInfo.CcDebugInfoCheck;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// consulo: top-level declarations of upstream CcDebugInfoPrompt.kt
public final class CcDebugInfoPrompt {
    private static final String DISMISSED_KEY = "bazel.cc.debug.info.dialog.dismissed";

    private static final Key<CcDebugInfoDialogTestHandler> TEST_HANDLER_KEY = Key.create("bazel.cc.debug.info.dialog.test.handler");

    private CcDebugInfoPrompt() {
    }

    /**
     * Answers the debug info dialog in a test.
     */
    // consulo: upstream public top-level fun interface of CcDebugInfoPrompt.kt
    @FunctionalInterface
    public interface CcDebugInfoDialogTestHandler {

        /**
         * @param canUseDbg true if the dialog shows the option to set {@code --compilation_mode=dbg}
         */
        CcDebugInfoChoice choose(CcDebugInfoCheck.Result result, boolean canUseDbg);
    }

    /**
     * Sets the {@code handler} that answers the debug info dialog of the project. Null shows the dialog again.
     */
    @TestOnly
    public static void setCcDebugInfoDialogHandlerInTests(Project project, @Nullable CcDebugInfoDialogTestHandler handler) {
        project.putUserData(TEST_HANDLER_KEY, handler);
    }

    static boolean isCcDebugInfoDialogDismissed(Project project) {
        return ProjectPropertiesComponent.getInstance(project).getBoolean(DISMISSED_KEY);
    }

    /**
     * Hides the debug info dialog for the project from now on.
     */
    static void dismissCcDebugInfoDialog(Project project) {
        ProjectPropertiesComponent.getInstance(project).setValue(DISMISSED_KEY, true);
    }

    /**
     * Asks the user to configure the project for debugging after the {@link CcDebugInfoCheck} found targets without debug info.
     * The frontend shows the dialog.
     *
     * @param projectViewFileName the name of the project view file that the dialog links to
     * @param canUseDbg           true shows the option to set {@code --compilation_mode=dbg}
     */
    // consulo: upstream suspend fun, the future completes with the choice
    static CompletableFuture<CcDebugInfoChoice> askCcDebugInfoChoice(
        Project project,
        Label mainTarget,
        CcDebugInfoCheck.Result result,
        String projectViewFileName,
        boolean canUseDbg
    ) {
        CcDebugInfoDialogTestHandler testHandler = project.getUserData(TEST_HANDLER_KEY);
        if (testHandler != null) {
            return CompletableFuture.completedFuture(testHandler.choose(result, canUseDbg));
        }

        // the repo mapping is in the workspace of the backend
        String targetName = BazelRepoMappingUtils.toShortString(mainTarget, project);
        return project.getInstance(CcDebugInfoPromptService.class).ask(answer ->
            buildCcDebugInfoRequest(targetName, result, projectViewFileName, canUseDbg, answer)
        );
    }

    /**
     * Collects the information on the backend required by the frontend to show the dialog.
     */
    public static CcDebugInfoRequest buildCcDebugInfoRequest(
        String targetName,
        CcDebugInfoCheck.Result result,
        String projectViewFileName,
        boolean canUseDbg,
        CompletableFuture<CcDebugInfoChoice> answer
    ) {
        List<Label> targets = result.issues().stream().map(CcDebugInfoCheck.Issue::label).distinct().toList();

        // a transition can set the compilation mode of a target, thus one build can mix the modes
        List<Label> withoutDbg = result.issues().stream()
            .filter(it -> it.compilationMode() == CcDebugInfo.CompilationMode.FASTBUILD ||
                it.compilationMode() == CcDebugInfo.CompilationMode.OPT)
            .map(CcDebugInfoCheck.Issue::label)
            .distinct()
            .toList();

        return new CcDebugInfoRequest(
            /* targetName = */ targetName,
            /* targetCount = */ targets.size(),
            /* targetsWithoutDbgCount = */ withoutDbg.size(),
            /* details = */ result.issues().stream().map(CcDebugInfoCheck.Issue::toString).toList(),
            // with dbg the cause is in the flags, thus the details are the only hint
            /* expandDetails = */ !result.isFixableByCompilationMode(),
            /* projectViewFileName = */ projectViewFileName,
            /* canUseDbg = */ canUseDbg,
            /* answer = */ answer
        );
    }
}
