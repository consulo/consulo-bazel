package org.jetbrains.bazel.clion.run.debug;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.bazel.cpp.localize.BazelCLionFrontendLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoPromptService.CcDebugInfoChoice;
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoPromptService.CcDebugInfoRequest;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * consulo: Consulo has no split mode (frontend/backend RPC). Upstream sends the request through a channel to the frontend
 * ({@code CcDebugInfoApi.requests}, module intellij.bazel.clion.frontend: {@code CcDebugInfoFrontendService},
 * {@code CcDebugInfoDialog}), which shows the dialog and sends the choice back. Here {@link #ask} shows the dialog itself
 * ({@link CcDebugInfoDialog} below), so the frontend timeout ({@code FRONTEND_TIMEOUT}) and {@code CcDebugInfoApiImpl}
 * (the {@code platform.rpc.backend.remoteApi} of the upstream XML) are dropped. The shared types of the upstream module
 * intellij.bazel.clion.shared ({@link CcDebugInfoChoice}, {@link CcDebugInfoRequest}) are nested here.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class CcDebugInfoPromptService {
    private static final Logger LOG = Logger.getInstance(CcDebugInfoPromptService.class);

    // consulo: upstream enum of intellij.bazel.clion.shared (org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice)
    public enum CcDebugInfoChoice {
        OPEN_PROJECT_VIEW,
        USE_DBG,
        DEBUG_ANYWAY,
        ALWAYS_DEBUG,
        CANCEL,
    }

    /**
     * Asks the frontend to show the debug info dialog. The backend computes all values.
     *
     * @param targetName             the short label of the debugged target
     * @param targetCount            the number of targets without debug info
     * @param targetsWithoutDbgCount the number of targets without debug info that Bazel built without {@code --compilation_mode=dbg}
     * @param details                one line for each issue
     * @param expandDetails          true if the details are the only hint, because {@code --compilation_mode=dbg} cannot fix the issues
     * @param projectViewFileName    the name of the project view file that the dialog links to
     * @param canUseDbg              true shows the option to set {@code --compilation_mode=dbg}
     * @param answer                 receives the choice of the user. The backend waits on it.
     */
    // consulo: upstream data class of intellij.bazel.clion.shared (org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest);
    //   the answer SendChannel<CcDebugInfoChoice> is a CompletableFuture
    public record CcDebugInfoRequest(
        String targetName,
        int targetCount,
        int targetsWithoutDbgCount,
        List<String> details,
        boolean expandDetails,
        String projectViewFileName,
        boolean canUseDbg,
        CompletableFuture<CcDebugInfoChoice> answer
    ) {
    }

    private final Project myProject;

    @Inject
    public CcDebugInfoPromptService(Project project) {
        myProject = project;
    }

    /**
     * Returns the answer of the user to the request that {@code createRequest} makes.
     * If the dialog cannot be shown, returns {@link CcDebugInfoChoice#DEBUG_ANYWAY}.
     * If the dialog is closed without a choice, returns {@link CcDebugInfoChoice#CANCEL}.
     */
    // consulo: upstream suspend fun ask(createRequest: (answer: SendChannel<CcDebugInfoChoice>) -> CcDebugInfoRequest)
    public CompletableFuture<CcDebugInfoChoice> ask(Function<CompletableFuture<CcDebugInfoChoice>, CcDebugInfoRequest> createRequest) {
        CompletableFuture<CcDebugInfoChoice> answer = new CompletableFuture<>();
        CcDebugInfoRequest request = createRequest.apply(answer);

        // consulo: upstream CcDebugInfoFrontendService.ask(request) + respond(request, choice) of the frontend
        myProject.getUIAccess().giveAsync(() -> CcDebugInfoDialog.show(myProject, request))
            .thenCompose(it -> it)
            .whenComplete((choice, error) -> {
                if (error != null) {
                    LOG.warn("Could not show the debug info dialog", error);
                    request.answer().complete(CcDebugInfoChoice.DEBUG_ANYWAY);
                }
                else {
                    request.answer().complete(choice);
                }
            });

        // consulo: upstream closes the answer channel in `finally`; a canceled debug session cancels the returned future instead
        return answer;
    }
}

/**
 * Asks the user to configure the project for debugging. The backend found targets without debug info.
 * <p>
 * consulo: port of upstream intellij.bazel.clion.frontend {@code CcDebugInfoDialog.kt} (a {@code DialogWrapper} built with the Kotlin
 * UI DSL) on {@link MessageBoxBuilder}: the buttons become message box buttons with the roles closest to their meaning, the link of
 * the instruction is the {@code OPEN_PROJECT_VIEW} choice, the collapsible details group is the detail text of the box.
 */
final class CcDebugInfoDialog {
    private static final String OPEN_PROJECT_VIEW_HREF = "open";

    private CcDebugInfoDialog() {
    }

    // consulo: upstream showCcDebugInfoDialog(project, request), withContext(Dispatchers.EDT) is done by the caller
    @RequiredUIAccess
    static CompletableFuture<CcDebugInfoChoice> show(Project project, CcDebugInfoRequest request) {
        AtomicReference<CcDebugInfoChoice> linkChoice = new AtomicReference<>();
        AtomicReference<CompletableFuture<CcDebugInfoChoice>> shown = new AtomicReference<>();

        LocalizeValue text = LocalizeValue.join(
            BazelCLionFrontendLocalize.debugInfoDialogHeader(XmlStringUtil.escapeText(request.targetName())),
            LocalizeValue.of("<br><br>"),
            issuesMessage(request),
            LocalizeValue.of("<br><br>"),
            BazelCLionFrontendLocalize.debugInfoDialogInstruction(XmlStringUtil.escapeText(request.projectViewFileName()))
        );
        String details = String.join("\n", request.details());

        MessageBoxBuilder<CcDebugInfoChoice> box = MessageBoxBuilder.create();
        box.title(BazelCLionFrontendLocalize.debugInfoDialogTitle());
        box.asWarning();
        box.richText(href -> {
            if (OPEN_PROJECT_VIEW_HREF.equals(href)) {
                // consulo: upstream close(OPEN_PROJECT_VIEW_EXIT_CODE); cancelling the result closes the box
                linkChoice.set(CcDebugInfoChoice.OPEN_PROJECT_VIEW);
                CompletableFuture<CcDebugInfoChoice> future = shown.get();
                if (future != null) {
                    future.cancel(false);
                }
            }
        });

        if (request.expandDetails()) {
            // consulo: upstream expands the collapsible details group, here the details are part of the text
            text = LocalizeValue.join(
                text,
                LocalizeValue.of("<br><br><b>"),
                BazelCLionFrontendLocalize.debugInfoDialogDetails(),
                LocalizeValue.of("</b><br><code>"),
                LocalizeValue.of(XmlStringUtil.escapeText(details).replace("\n", "<br>")),
                LocalizeValue.of("</code>")
            );
        }
        else {
            box.detail(LocalizeValue.of(details));
        }
        box.text(text);

        // the quick fix is the primary action if there is one
        if (request.canUseDbg()) {
            box.button(MessageButtonRole.RETRY, BazelCLionFrontendLocalize.debugInfoDialogUseDbg(), CcDebugInfoChoice.USE_DBG);
            box.asDefaultButton();
        }

        // to continue without a fix is the alternative to the actions on the right
        box.button(MessageButtonRole.OK, BazelCLionFrontendLocalize.debugInfoDialogDebugAnyway(), CcDebugInfoChoice.DEBUG_ANYWAY);
        if (!request.canUseDbg()) {
            box.asDefaultButton();
        }

        // consulo: upstream option of the DebugAnywayAction (OptionAction), here a button of its own.
        //   The option also hides the dialog for the project from now on.
        box.button(
            MessageButtonRole.YES_TO_ALL,
            BazelCLionFrontendLocalize.debugInfoDialogDebugAlways(),
            CcDebugInfoChoice.ALWAYS_DEBUG
        );

        box.button(MessageButtonRole.CANCEL, CcDebugInfoChoice.CANCEL);
        box.asExitButton();

        CompletableFuture<CcDebugInfoChoice> result = box.showAsync((Window) null);
        shown.set(result);

        return result.handle((choice, error) -> {
            CcDebugInfoChoice fromLink = linkChoice.get();
            if (fromLink != null) {
                return fromLink;
            }
            if (error != null) {
                if (error instanceof CancellationException || error.getCause() instanceof CancellationException) {
                    return CcDebugInfoChoice.CANCEL;
                }
                throw error instanceof CompletionException completionException ? completionException : new CompletionException(error);
            }
            return choice == null ? CcDebugInfoChoice.CANCEL : choice;
        });
    }

    /**
     * Counts the targets without debug info. Names the compilation mode as the cause if a target is known to be built without dbg.
     */
    private static LocalizeValue issuesMessage(CcDebugInfoRequest request) {
        if (request.targetsWithoutDbgCount() == 0) {
            return BazelCLionFrontendLocalize.debugInfoDialogTargets(request.targetCount());
        }
        else if (request.targetsWithoutDbgCount() == request.targetCount()) {
            return BazelCLionFrontendLocalize.debugInfoDialogTargetsAllNotDbg(request.targetCount());
        }
        else {
            return BazelCLionFrontendLocalize.debugInfoDialogTargetsSomeNotDbg(request.targetCount(), request.targetsWithoutDbgCount());
        }
    }
}
