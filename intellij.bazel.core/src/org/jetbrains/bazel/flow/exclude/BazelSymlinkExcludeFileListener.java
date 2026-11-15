package org.jetbrains.bazel.flow.exclude;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class BazelSymlinkExcludeFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(List<? extends VFileEvent> events) {
        Map<Project, List<Path>> project2Paths = new LinkedHashMap<>();
        for (VFileEvent event : events) {
            if (!(event instanceof VFileCreateEvent createEvent)) {
                continue;
            }
            FileAttributes attributes = createEvent.getAttributes();
            if (!(attributes != null && attributes.isSymLink())) {
                continue;
            }
            for (ProjectAndPath it : toListOfProjectAndPath(createEvent)) {
                if (BazelSymlinksCalculator.isBazelSymlink(org.jetbrains.bazel.config.project.getBazelProjectName(it.project()), it.path())) {
                    project2Paths.computeIfAbsent(it.project(), k -> new ArrayList<>()).add(it.path());
                }
            }
        }
        return new ChangeApplier() {
            @Override
            public void beforeVfsChange() {
                for (Map.Entry<Project, List<Path>> entry : project2Paths.entrySet()) {
                    BazelSymlinkExcludeService bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(entry.getKey());
                    bazelSymlinkExcludeService.addBazelSymlinksToExclude(new HashSet<>(entry.getValue()));
                }
            }
        };
    }

    private List<ProjectAndPath> toListOfProjectAndPath(VFileCreateEvent receiver) {
        Path nioPath = toNioPathOrNull(receiver.getPath());
        if (nioPath == null) {
            return List.of();
        }
        List<Project> projects = findProjectsByFilePath(nioPath);
        List<ProjectAndPath> result = new ArrayList<>(projects.size());
        for (Project it : projects) {
            result.add(new ProjectAndPath(it, nioPath));
        }
        return result;
    }

    private List<Project> findProjectsByFilePath(Path path) {
        List<Project> result = new ArrayList<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed() || !org.jetbrains.bazel.config.project.isBazelProject(project)) {
                continue;
            }
            // we can't use content roots, etc. because from the observations it turns out that
            // an event can be dispatched when a project doesn't have any modules or content roots.
            String basePath = project.getBasePath();
            Path projectBasePath = basePath != null ? toNioPathOrNull(basePath) : null;
            if (projectBasePath == null) {
                continue;
            }
            if (path.startsWith(projectBasePath)) {
                result.add(project);
            }
        }
        return result;
    }

    // consulo: IntelliJ String.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(String path) {
        try {
            return Path.of(path);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    private record ProjectAndPath(Project project, Path path) {
    }
}
