package org.jetbrains.bazel.golang.sync;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.utils.PathUtil;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class GoExternalLibraryManager {
    private final Project myProject;

    private volatile GoExternalSyntheticLibrary myLibrary;

    @Inject
    public GoExternalLibraryManager(Project project) {
        myProject = project;
        myLibrary = createExternalLibrary();
    }

    public GoExternalSyntheticLibrary getLibrary() {
        return myLibrary;
    }

    public void update() {
        GoExternalSyntheticLibrary newLibrary = createExternalLibrary();
        synchronized (this) {
            if (myLibrary.equals(newLibrary)) {
                return;
            }
            myLibrary = newLibrary;
        }
        fireLibraryChanged(newLibrary);
    }

    private GoExternalSyntheticLibrary createExternalLibrary() {
        List<VirtualFile> files = getLibraryFiles(myProject).stream()
            .map(PathUtil::findVirtualFile)
            .filter(Objects::nonNull)
            .toList();
        return new GoExternalSyntheticLibrary(files);
    }

    private static List<Path> getLibraryFiles(Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return List.of();
        }
        VirtualFile rootDir = org.jetbrains.bazel.config.project.getRootDir(project);
        // consulo: VirtualFile.toNioPathOrNull()
        Path workspacePath = rootDir.getFileSystem().getNioPath(rootDir);
        if (workspacePath == null) {
            return List.of();
        }

        // consulo: upstream reads the BazelGoPackageEntity entities of the workspace model; the sources are VFS urls
        List<Path> libraryFiles =
            GoPackageEntities.getInstance(project)
                .getPackages()
                .stream()
                .flatMap((BazelGoPackageEntity it) -> it.sources().stream())
                .map(it -> Path.of(VirtualFileUtil.urlToPath(it)))
                // Files inside the workspace are handled by GoWorkspaceImporter
                .filter(it -> !it.startsWith(workspacePath) && FileUtil.getExtension(it.getFileName().toString()).equals("go"))
                .distinct()
                .sorted()  // Make sure resyncs without changes produce the same result
                .toList();
        return libraryFiles;
    }

    private void fireLibraryChanged(GoExternalSyntheticLibrary library) {
        // TODO consulo: AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(project, library.presentableText, emptyList(),
        //  library.allRoots, GoExternalLibraryManager::class.java.name) is missing; Consulo has no public API to fire a roots change,
        //  so GoAdditionalLibraryRootsProvider (an IndexableSetContributor) sees the new library on the next roots change
    }

    public static GoExternalLibraryManager getInstance(Project project) {
        return project.getInstance(GoExternalLibraryManager.class);
    }
}
