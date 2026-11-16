package com.intellij.bazel.python.backend;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.workspace.indexing.IndexableContent;
import org.jetbrains.bazel.workspace.indexing.IndexableContentContributor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtensionImpl
public class PythonIndexableContentContributor implements IndexableContentContributor {

    @Override
    public IndexableContent getIndexableContent(Project project) {
        Path rootDir = Path.of(org.jetbrains.bazel.config.project.getRootDir(project).getPath());
        Map<?, Path> sourcesIndex = project.getInstance(PythonResolveIndexService.class).getResolveIndex();
        // consulo: the roots are VFS urls instead of workspace model VirtualFileUrls (no VirtualFileUrlManager)
        Set<String> nonRecursiveRoots = new LinkedHashSet<>();
        for (Path path : sourcesIndex.values()) {
            if (path.startsWith(rootDir)) {
                continue;
            }
            // non-recursive root does not include children of a directory, so add the `__init__` file of a package explicitly
            VirtualFile directory = VirtualFileManager.getInstance().findFileByNioPath(path);
            List<VirtualFile> packageInitFiles = directory != null && directory.isDirectory()
                ? PythonResolveIndexService.findPackageInitFiles(directory)
                : List.of();
            List<Path> paths = new ArrayList<>();
            paths.add(path);
            for (VirtualFile it : packageInitFiles) {
                paths.add(it.toNioPath());
            }
            for (Path it : paths) {
                nonRecursiveRoots.add(VirtualFileUtil.pathToUrl(it.toString()));
            }
        }
        return IndexableContent.of(Set.of(), nonRecursiveRoots);
    }
}
