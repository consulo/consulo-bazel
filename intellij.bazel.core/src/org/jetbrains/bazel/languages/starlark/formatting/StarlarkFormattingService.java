package org.jetbrains.bazel.languages.starlark.formatting;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.ExternalFormatProcessor;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.util.PsiEditorUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.util.CapturingProcessAdapter;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.settings.bazel.BazelProjectSettings;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// TODO consulo: IntelliJ AsyncDocumentFormattingService (with AsyncFormattingRequest/FormattingTask) does not exist in Consulo.
//  The service is registered as the public ExternalFormatProcessor extension and runs buildifier synchronously
//  (AsyncFormattingRequest/FormattingTask are minimal private replacements below). Consulo only calls
//  ExternalFormatProcessor.format() from CodeStyleManager.reformat()/reformatRange(); reformatText() (Reformat Code action)
//  skips the built-in formatter for such files but has no post-format adapter calling the external formatter.
@ExtensionImpl
public class StarlarkFormattingService implements ExternalFormatProcessor {
    private static final Logger LOG = Logger.getInstance(StarlarkFormattingService.class);
    private static final String NOTIFICATION_GROUP_ID = "Buildifier";

    // consulo: the synchronous run is bounded (upstream ran the task asynchronously under progress)
    private static final long BUILDIFIER_TIMEOUT_MILLIS = 30_000;

    // consulo: getFeatures() (upstream emptySet()) has no ExternalFormatProcessor counterpart; the whole file is always formatted

    public boolean canFormat(PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        if (!FileTypeRegistry.getInstance().isFileOfType(virtualFile, StarlarkFileType.INSTANCE)) {
            return false;
        }
        // consulo: upstream TrustedProjects.isProjectTrusted check is dropped, Consulo has no trusted projects
        return BazelProjectSettings.getBazelProjectSettings(file.getProject()).getBuildifierPathString(file.getProject()) != null;
    }

    private @Nullable FormattingTask createFormattingTask(AsyncFormattingRequest request) {
        FormattingContext formattingContext = request.getContext();
        Project project = formattingContext.getProject();
        // consulo: upstream TrustedProjects.isProjectTrusted check is dropped, Consulo has no trusted projects
        String buildifierPath =
            BazelProjectSettings.getBazelProjectSettings(project).getBuildifierPathString(request.getContext().getProject());
        if (buildifierPath == null) {
            return null;
        }

        if (!checkDocumentExists(request)) {
            LOG.warn("Document for file " + request.getContext().getContainingFile().getName() + " is null");
            return null;
        }

        ProcessHandler handler = createProcessHandler(request, buildifierPath);
        if (handler == null) {
            return null;
        }

        return new FormattingTask() {
            @Override
            public void run() {
                try {
                    handler.addProcessListener(new BuildifierProcessListener(request));
                    handler.startNotify();
                    // consulo: upstream passed the text with GeneralCommandLine.withInput(ioFile); Consulo has no withInput()
                    writeInput(handler, request.getDocumentText());
                    // consulo: wait for the process, the formatting runs synchronously
                    if (!handler.waitFor(BUILDIFIER_TIMEOUT_MILLIS)) {
                        cancel();
                        request.onTextReady(null);
                    }
                }
                catch (Exception e) {
                    LOG.warn(String.valueOf(e.getLocalizedMessage()));
                    request.onTextReady(null);
                }
            }

            @Override
            public boolean cancel() {
                handler.destroyProcess();
                return true;
            }

            @Override
            public boolean isRunUnderProgress() {
                return true;
            }
        };
    }

    private boolean checkDocumentExists(AsyncFormattingRequest request) {
        PsiFile psiFile = request.getContext().getContainingFile();
        Project project = request.getContext().getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

        return document != null;
    }

    private @Nullable ProcessHandler createProcessHandler(AsyncFormattingRequest request, String buildifierPath) {
        Project project = request.getContext().getProject();
        // Formatter can be called for non-Bazel projects as well (if they contain some Starlark).
        // Don't use project.rootDir as it will throw.
        VirtualFile rootDir = project.getInstance(BazelProjectInfoService.class).getProjectRootDir();
        if (rootDir == null) {
            // consulo: Project.guessProjectDir() -> Project.getBaseDir()
            rootDir = project.getBaseDir();
        }
        if (rootDir == null) {
            return null;
        }
        GeneralCommandLine commandLine =
            new GeneralCommandLine()
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withExePath(buildifierPath)
                // consulo: buildifier reads the document text from stdin (see FormattingTask.run()), always as UTF-8
                .withCharset(StandardCharsets.UTF_8);
        // Because we pass the input file's text into stdin, buildifier doesn't know the original filename.
        // We must therefore tell buildifier what file type it was.
        commandLine.addParameter("--type=" + fileTypeArgument(request.getContext().getContainingFile().getName()));
        // Set the work directory to workspace root so that Buildifier can read .buildifier-tables.json
        commandLine.withWorkingDirectory(rootDir.toNioPath());

        try {
            return Application.get().getInstance(ProcessHandlerBuilderFactory.class).newBuilder(commandLine).build();
        }
        catch (ExecutionException e) {
            // consulo: upstream CapturingProcessHandler(commandLine) let the exception propagate
            LOG.warn(String.valueOf(e.getLocalizedMessage()));
            return null;
        }
    }

    // consulo: takes the original file name instead of the upstream temporary io file (there is no io file, the text goes to stdin)
    private String fileTypeArgument(String fileName) {
        return switch (BazelFileType.ofFileName(fileName)) {
            case BUILD -> "build";
            case EXTENSION -> "bzl";
            case MODULE -> "module";
            case WORKSPACE -> "workspace";
        };
    }

    public String getNotificationGroupId() {
        return NOTIFICATION_GROUP_ID;
    }

    public LocalizeValue getName() {
        return BazelPluginLocalize.buildifierFormattingServiceName();
    }

    // consulo: ExternalFormatProcessor implementation (replaces the AsyncDocumentFormattingService plumbing)

    @Override
    public boolean activeForFile(PsiFile source) {
        return canFormat(source);
    }

    @Override
    @RequiredWriteAction
    public @Nullable TextRange format(PsiFile source, TextRange range, boolean canChangeWhiteSpacesOnly) {
        Document document = PsiDocumentManager.getInstance(source.getProject()).getDocument(source);
        if (document == null) {
            return null;
        }
        TextRange textRange = TextRange.from(0, source.getTextLength());
        FormattingContext formattingContext =
            FormattingContext.create(source, textRange, CodeStyle.getSettings(source), FormattingMode.REFORMAT);
        if (!formatDocument(document, List.of(textRange), formattingContext, canChangeWhiteSpacesOnly, false)) {
            return null;
        }
        return TextRange.from(0, document.getTextLength());
    }

    @Override
    public @Nullable String indent(PsiFile source, int lineStartOffset) {
        return null;
    }

    @Override
    public String getId() {
        return NOTIFICATION_GROUP_ID;
    }

    /**
     * consulo: replaces IntelliJ {@code AsyncDocumentFormattingService.formatDocument()}: runs buildifier synchronously and
     * replaces the document text with the result.
     *
     * @return true if the document was formatted
     */
    @RequiredWriteAction
    public boolean formatDocument(
        Document document,
        List<TextRange> formattingRanges,
        FormattingContext formattingContext,
        boolean canChangeWhiteSpaceOnly,
        boolean quickFormat
    ) {
        AsyncFormattingRequest request = new AsyncFormattingRequest(formattingContext, document.getText());
        FormattingTask task = createFormattingTask(request);
        if (task == null) {
            return false;
        }
        task.run();
        String formattedText = request.getFormattedText();
        if (formattedText == null) {
            return false;
        }
        if (!formattedText.equals(document.getText())) {
            document.replaceString(0, document.getTextLength(), formattedText);
        }
        return true;
    }

    @RequiredWriteAction
    public static void formatBuildFile(StarlarkFile buildFile) {
        // consulo: upstream suspend readAndWriteAction { writeAction { ... } }; runs synchronously in the caller's write action
        StarlarkFormattingService formattingService = new StarlarkFormattingService();
        TextRange textRange = TextRange.from(0, buildFile.getTextLength());
        FormattingContext formattingContext =
            FormattingContext.create(buildFile, textRange, CodeStyle.getSettings(buildFile), FormattingMode.REFORMAT);
        formattingService.formatDocument(buildFile.getContainingFile().getFileDocument(), List.of(textRange), formattingContext, true,
            true);
    }

    // consulo: upstream GeneralCommandLine.withInput(ioFile)
    private static void writeInput(ProcessHandler handler, String text) throws IOException {
        OutputStream input = handler.getProcessInput();
        if (input == null) {
            return;
        }
        try (OutputStream stream = input) {
            stream.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * consulo: minimal replacement of IntelliJ {@code AsyncDocumentFormattingService.FormattingTask}
     */
    private interface FormattingTask extends Runnable {
        boolean cancel();

        boolean isRunUnderProgress();
    }

    /**
     * consulo: minimal replacement of IntelliJ {@code AsyncFormattingRequest}
     */
    private static final class AsyncFormattingRequest {
        private final FormattingContext myContext;
        private final String myDocumentText;
        private @Nullable String myFormattedText;

        private AsyncFormattingRequest(FormattingContext context, String documentText) {
            myContext = context;
            myDocumentText = documentText;
        }

        FormattingContext getContext() {
            return myContext;
        }

        String getDocumentText() {
            return myDocumentText;
        }

        @Nullable String getFormattedText() {
            return myFormattedText;
        }

        void onTextReady(@Nullable String updatedText) {
            myFormattedText = updatedText;
        }

        void onError(LocalizeValue title, String message) {
            myFormattedText = null;
            // consulo: the "Buildifier" notification group is not registered upstream either; errors go to the BazelPlugin group
            NotificationService.getInstance()
                .newError(BazelNotificationGroupContributor.BAZEL_PLUGIN)
                .title(title)
                .content(LocalizeValue.of(message))
                .notify(myContext.getProject());
        }
    }

    private static class BuildifierProcessListener extends CapturingProcessAdapter {
        private final AsyncFormattingRequest myRequest;

        private BuildifierProcessListener(AsyncFormattingRequest request) {
            myRequest = request;
        }

        @Override
        public void processTerminated(ProcessEvent event) {
            int exitCode = event.getExitCode();
            if (exitCode == 0) {
                showFormattedOutput();
            }
            else {
                myRequest.onError(BazelPluginLocalize.buildifierRunErrorMessage(), getOutput().getStderr());
            }
        }

        private void showFormattedOutput() {
            if (getOutput().getStdout().isEmpty()) {
                showFormattedLinesInfo(BazelPluginLocalize.buildifierFormattedIgnored());
                myRequest.onTextReady(null);
            }
            else {
                myRequest.onTextReady(getOutput().getStdout());
            }
        }

        private void showFormattedLinesInfo(LocalizeValue text) {
            Application.get().invokeLater(
                () -> {
                    Editor editor = PsiEditorUtil.findEditor(myRequest.getContext().getContainingFile());
                    if (editor == null) {
                        return;
                    }
                    // consulo: HintManagerImpl.showEditorHint(LightweightHint(HintUtil.createInformationLabel(text)), editor, ABOVE,
                    //  HIDE_BY_ANY_KEY | HIDE_BY_SCROLLING, 0, false) -> the public HintManager information hint
                    HintManager.getInstance().showInformationHint(editor, text, HintManager.ABOVE);
                },
                Application.get().getDefaultModalityState()
            );
        }
    }
}
