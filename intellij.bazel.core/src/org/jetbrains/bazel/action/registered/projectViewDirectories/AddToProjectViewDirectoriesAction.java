package org.jetbrains.bazel.action.registered.projectViewDirectories;

import consulo.annotation.component.ActionImpl;
import consulo.application.ReadAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFileDirectories;
import org.jetbrains.bazel.project.DefaultProjectViewService;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.utils.AnActionEventUtils;
import org.jetbrains.bazel.utils.VfsUtils;
import org.jetbrains.bazel.workspace.BazelProjectDirectoriesUtils;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@ActionImpl(id = "Bazel.AddToProjectViewDirectoriesAction")
public class AddToProjectViewDirectoriesAction extends AnAction implements AnActionWithSyncUpdate {
    public AddToProjectViewDirectoriesAction() {
        // consulo: the template text needs its argument, update() sets the real project view file name
        super(
            BazelPluginLocalize.actionBazelAddtoprojectviewdirectoriesactionText(BazelPluginLocalize.toolWindowGenericConfigFile()),
            BazelPluginLocalize.actionBazelAddtoprojectviewdirectoriesactionDescription()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile directory = AnActionEventUtils.getSelectedDirectory(e);
        if (directory == null) {
            return;
        }
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        ProjectViewPsiFile projectViewPsi = DefaultProjectViewService.getProjectViewFilePsiFile(project);
        if (projectViewPsi == null) {
            return;
        }
        Set<VirtualFile> includes = explicitlyIncludedRoots(project);
        Set<VirtualFile> excludedRoots = BazelProjectDirectoriesUtils.excludedRoots(project);
        Set<VirtualFile> excludes = excludedRoots != null ? excludedRoots : Set.of();
        Set<VirtualFile> candidates = new HashSet<>(includes);
        candidates.addAll(excludes);
        candidates.remove(directory);
        VirtualFile nearestParent = VfsUtils.findNearestParent(directory, candidates);
        boolean isNotIncluded = nearestParent == null || !includes.contains(nearestParent);
        boolean isExplicitlyExcluded = excludes.contains(directory);
        if (!isExplicitlyExcluded && !isNotIncluded) {
            return;
        }
        projectViewPsi.navigate(/* requestFocus = */ true);
        if (ProjectViewPsiFileDirectories.directoriesContainsInclude(projectViewPsi, directory)) {
            return;
        }
        // consulo: Consulo takes the project from the file (upstream writeCommandAction(project, projectViewPsi))
        WriteCommandAction.writeCommandAction(projectViewPsi).run((ThrowableRunnable<RuntimeException>) () -> {
            if (ProjectViewPsiFileDirectories.isDirectoriesNullOrEmpty(projectViewPsi)) {
                ProjectViewPsiFileDirectories.addDirectoriesInclude(projectViewPsi, org.jetbrains.bazel.config.project.getRootDir(project));
            }
            if (isExplicitlyExcluded) {
                ProjectViewPsiFileDirectories.removeDirectoriesExclude(projectViewPsi, directory);
            }
            if (isNotIncluded) {
                ProjectViewPsiFileDirectories.addDirectoriesInclude(projectViewPsi, directory);
            }
            PsiDocumentManager
                .getInstance(project)
                .commitAllDocuments();
        });
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile projectViewFile = project != null ? getProjectViewIfApplicableTo(project, e) : null;
        VirtualFile rootDir = project != null ? project.getInstance(BazelProjectInfoService.class).getProjectRootDir() : null;
        if (projectViewFile == null || rootDir == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setText(BazelPluginLocalize.actionBazelAddtoprojectviewdirectoriesactionText(projectViewFile.getName()));
    }

    // consulo: AnActionWithSyncUpdate always updates in background (upstream ActionUpdateThread.BGT)

    private @Nullable VirtualFile getProjectViewIfApplicableTo(Project receiver, AnActionEvent e) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(receiver)) {
            return null;
        }
        if (DumbService.isDumb(receiver)) {
            return null;
        }
        VirtualFile directory = AnActionEventUtils.getSelectedDirectory(e);
        if (directory == null) {
            return null;
        }
        Set<VirtualFile> includes = explicitlyIncludedRoots(receiver);
        Set<VirtualFile> excludedRoots = BazelProjectDirectoriesUtils.excludedRoots(receiver);
        Set<VirtualFile> excludes = excludedRoots != null ? excludedRoots : Set.of();
        if (includes.contains(directory)) {
            return null;
        }
        else if (excludes.contains(directory)) {
            return DefaultProjectViewService.getProjectViewFile(receiver);
        }
        else {
            Set<VirtualFile> candidates = new HashSet<>(includes);
            candidates.addAll(excludes);
            VirtualFile nearestParent = VfsUtils.findNearestParent(directory, candidates);
            if (nearestParent != null && includes.contains(nearestParent)) {
                return null;
            }
            else {
                return DefaultProjectViewService.getProjectViewFile(receiver);
            }
        }
    }

    public static Set<VirtualFile> explicitlyIncludedRoots(Project receiver) {
        if (ReadAction.nonBlocking(() -> {
            ProjectViewPsiFile projectViewPsiFile = DefaultProjectViewService.getProjectViewFilePsiFile(receiver);
            return projectViewPsiFile != null && ProjectViewPsiFileDirectories.isDirectoriesNullOrEmpty(projectViewPsiFile);
        }).executeSynchronously()) {
            return Set.of();
        }
        Set<VirtualFile> includedRoots = BazelProjectDirectoriesUtils.includedRoots(receiver);
        return includedRoots != null ? includedRoots : Set.of();
    }
}
