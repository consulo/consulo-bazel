package org.jetbrains.bazel.project;

import consulo.annotation.component.ServiceImpl;
import consulo.application.WriteAction;
import consulo.bazel.core.BazelProjectStoreService;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jetbrains.bazel.languages.projectview.imports.ImportFactory;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ServiceImpl
@Singleton
public class DefaultProjectViewService implements ProjectViewService {
    private static final Logger LOG = Logger.getInstance(DefaultProjectViewService.class);

    private final Project myProject;

    private final LazyValue<VirtualFile> myProjectViewFile = LazyValue.atomicNotNull(this::requireProjectViewVirtualFile);

    // consulo: StateFlow replaced by current value
    private volatile ProjectView myProjectViewState = ProjectView.EMPTY;

    @Inject
    public DefaultProjectViewService(Project project) {
        myProject = project;
    }

    public VirtualFile getProjectViewFile() {
        return myProjectViewFile.get();
    }

    @Override
    public ProjectView getProjectViewState() {
        return myProjectViewState;
    }

    public CompletableFuture<Void> ensureProjectViewInitialized() {
        if (myProjectViewState == ProjectView.EMPTY) {
            ProjectView newProjectView = computeProjectView();
            myProjectViewState = newProjectView;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> forceReparseCurrentProjectViewFiles() {
        LOG.info("Reparsing currently opened project view file");
        ProjectView newProjectView = computeProjectView();
        myProjectViewState = newProjectView;
        List<VirtualFile> resolvedImports = new ArrayList<>();
        for (Import it : newProjectView.getImports()) {
            Path resolvedPath = ImportFactory.resolvedPathOrNull(it);
            VirtualFile file = resolvedPath != null ? PathUtil.findVirtualFile(resolvedPath) : null;
            if (file != null) {
                resolvedImports.add(file);
            }
        }
        // consulo: upstream edtWriteAction { }
        return myProject.getUIAccess().giveAsync(() -> WriteAction.run(() -> {
            List<VirtualFile> files = new ArrayList<>(resolvedImports);
            files.add(getProjectViewFile());
            PsiDocumentManager.getInstance(myProject)
                .reparseFiles(files, false);
        })).<Void>thenApply(ignored -> null);
    }

    private ProjectView computeProjectView() {
        ProjectView projectView = ProjectViewFactory.fromOrNull(myProject, requireProjectViewPath());
        return projectView != null ? projectView : ProjectViewFactory.fromDefault(myProject);
    }

    private Path requireProjectViewPath() {
        Path projectViewPath = getProjectViewPath();
        if (projectViewPath == null) {
            throw new IllegalStateException("Missing project view path");
        }
        return projectViewPath;
    }

    private VirtualFile requireProjectViewVirtualFile() {
        VirtualFile file = PathUtil.findVirtualFile(requireProjectViewPath());
        if (file == null) {
            throw new IllegalStateException("Could not find project view file in VFS");
        }
        return file;
    }

    // Only to be used in monorepo devkit module
    public CompletableFuture<Void> forceLoadProjectViewFile(VirtualFile newFile) {
        LOG.info("Forcibly loading project view file content: " + newFile);
        myProjectViewState = ProjectViewFactory.from(myProject, newFile.toNioPath());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @Nullable Path getProjectViewPath() {
        // consulo: the Bazel store descriptor is persisted by BazelProjectStoreService (upstream project.stateStoreOrNull?.storeDescriptor)
        BazelProjectStoreDescriptor descriptor = BazelProjectStoreService.getStoreDescriptor(myProject);
        return descriptor != null ? descriptor.getProjectViewFile() : null;
    }

    public static DefaultProjectViewService getInstance(Project project) {
        return (DefaultProjectViewService) ProjectViewService.getInstance(project);
    }

    // consulo: upstream Kotlin extension property Project.projectViewFile
    public static VirtualFile getProjectViewFile(Project receiver) {
        return DefaultProjectViewService.getInstance(receiver).getProjectViewFile();
    }

    // consulo: upstream Kotlin extension property Project.projectViewFilePsiFile
    public static @Nullable ProjectViewPsiFile getProjectViewFilePsiFile(Project receiver) {
        PsiFile file = PsiManager.getInstance(receiver).findFile(getProjectViewFile(receiver));
        return file instanceof ProjectViewPsiFile projectViewPsiFile ? projectViewPsiFile : null;
    }
}
