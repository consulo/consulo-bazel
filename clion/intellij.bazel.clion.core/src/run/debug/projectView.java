package org.jetbrains.bazel.clion.run.debug;

import consulo.application.ReadAction;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.action.SaveAllFiles;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

// consulo: top-level declarations of upstream projectView.kt
public final class projectView {
    private projectView() {
    }

    /**
     * Returns null if the project has no project view file.
     */
    // consulo: upstream suspend fun with withContext(Dispatchers.IO) { }, runs in the calling (pooled) thread
    static @Nullable VirtualFile findProjectViewFile(Project project) {
        Path path = ProjectViewService.getInstance(project).getProjectViewPath();
        if (path == null) {
            return null;
        }
        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path);
    }

    static CompletableFuture<?> openProjectViewAtBuildFlags(Project project, VirtualFile file) {
        // consulo: upstream suspending readAction { }, runs as a blocking read action in the calling thread
        @Nullable Integer endOffset = ReadAction.<@Nullable Integer, RuntimeException>compute(() -> {
            ProjectViewPsiFile psi = findProjectViewPsi(project, file);
            return psi == null ? null : org.jetbrains.bazel.languages.projectview.psi.setCompilationMode.buildFlagsEndOffset(psi);
        });
        int offset = endOffset == null ? 0 : endOffset;

        // consulo: upstream withContext(Dispatchers.EDT) { }
        return project.getUIAccess().giveAsync(() ->
            OpenFileDescriptorFactory.getInstance(project).newBuilder(file).offset(offset).build().navigate(true)
        );
    }

    /**
     * Writes {@code --compilation_mode=dbg} to the {@code build_flags} of the project view file.
     * The server reads the new flags only after the reparse of the project view.
     */
    // consulo: @Throws(ExecutionException::class) context(ctx: CcDebugContext) suspend fun -> ctx parameter, the future fails with the
    //   ExecutionException
    static CompletableFuture<?> setDbgCompilationMode(CcDebugContext ctx) {
        VirtualFile file = findProjectViewFile(ctx.getProject());
        if (file == null) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelCLionCoreLocalize.debugErrorProjectViewNotFound()));
        }

        // consulo: upstream suspending writeCommandAction(project, name) { }, the command runs on the UI thread
        return ctx.getProject().getUIAccess().giveAsync(() -> {
            try {
                CommandProcessor.getInstance().newCommand()
                    .project(ctx.getProject())
                    .name(BazelCLionCoreLocalize.debugInfoCommandSetCompilationMode())
                    .inWriteAction()
                    .canThrow(ExecutionException.class)
                    .run(() -> {
                        ProjectViewPsiFile psi = findProjectViewPsi(ctx.getProject(), file);
                        if (psi == null) {
                            throw new ExecutionException(BazelCLionCoreLocalize.debugErrorProjectViewNotFound());
                        }

                        org.jetbrains.bazel.languages.projectview.psi.setCompilationMode.setCompilationMode(psi, "dbg");
                    });
            }
            catch (ExecutionException e) {
                throw new CompletionException(e);
            }
        })
            .thenCompose(ignored -> SaveAllFiles.saveAllFiles())
            .thenCompose(ignored -> ProjectViewService.getInstance(ctx.getProject()).forceReparseCurrentProjectViewFiles());
    }

    private static @Nullable ProjectViewPsiFile findProjectViewPsi(Project project, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return psiFile instanceof ProjectViewPsiFile projectViewPsiFile ? projectViewPsiFile : null;
    }
}
