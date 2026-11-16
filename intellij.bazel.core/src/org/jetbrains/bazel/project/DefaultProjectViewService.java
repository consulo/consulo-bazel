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
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.projectview.ProjectViewServiceBase;
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem;
import org.jetbrains.bazel.languages.projectview.imports.Import;
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@ServiceImpl
@Singleton
public class DefaultProjectViewService extends ProjectViewServiceBase {
    private static final Logger LOG = Logger.getInstance(DefaultProjectViewService.class);

    // consulo: kept next to the protected project of ProjectViewServiceBase, so the class does not depend on its Java name
    private final Project myProject;

    private final LazyValue<VirtualFile> myProjectViewFile = LazyValue.atomicNotNull(() -> {
        VirtualFile file = PathUtil.findVirtualFile(requireProjectViewPath());
        if (file == null) {
            throw new IllegalStateException("Could not find project view file in VFS");
        }
        return file;
    });

    @Inject
    public DefaultProjectViewService(Project project) {
        super(project);
        myProject = project;
    }

    public VirtualFile getProjectViewFile() {
        return myProjectViewFile.get();
    }

    @Override
    public @Nullable Path getProjectViewPath() {
        // consulo: the Bazel store descriptor is persisted by BazelProjectStoreService (upstream project.stateStoreOrNull?.storeDescriptor)
        BazelProjectStoreDescriptor descriptor = BazelProjectStoreService.getStoreDescriptor(myProject);
        return descriptor != null ? descriptor.getProjectViewFile() : null;
    }

    public void ensureProjectViewInitialized() {
        if (getProjectView() == ProjectView.EMPTY) {
            loadProjectView(requireProjectViewPath());
        }
    }

    // Only to be used in monorepo devkit module
    public void forceLoadProjectViewFile(VirtualFile newFile) {
        LOG.info("Forcibly loading project view file content: " + newFile);
        loadProjectView(newFile.toNioPath());
    }

    @Override
    public CompletableFuture<Void> forceReparseCurrentProjectViewFiles(@Nullable Consumer<ProjectViewProblem> onEachProblem) {
        loadProjectView(requireProjectViewPath(), onEachProblem);
        List<VirtualFile> resolvedImports = new ArrayList<>();
        for (Import.Resolved it : getProjectView().getImports()) {
            VirtualFile file = PathUtil.findVirtualFile(it.path());
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

    private Path requireProjectViewPath() {
        Path projectViewPath = getProjectViewPath();
        if (projectViewPath == null) {
            throw new IllegalStateException("Missing project view path");
        }
        return projectViewPath;
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
