package org.jetbrains.bazel.flow.exclude;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class BazelDirectoryIndexExcludePolicy implements DirectoryIndexExcludePolicy, DumbAware {
    private final Project myProject;
    // consulo: injected, the policy is first called inside the write action of a roots change
    private final BazelSymlinkExcludeService mySymlinkExcludeService;

    @Inject
    public BazelDirectoryIndexExcludePolicy(Project project, BazelSymlinkExcludeService symlinkExcludeService) {
        myProject = project;
        mySymlinkExcludeService = symlinkExcludeService;
    }

    public Project getProject() {
        return myProject;
    }

    // consulo: Consulo excludes VirtualFiles (upstream getExcludeUrlsForProject() returns the `Path.toUri()` urls)
    @Override
    public VirtualFile[] getExcludeRootsForProject() {
        List<VirtualFile> result = new ArrayList<>();
        for (Path it : mySymlinkExcludeService.getBazelSymlinksToExclude()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(it);
            if (file != null) {
                result.add(file);
            }
        }
        return result.toArray(VirtualFile.EMPTY_ARRAY);
    }

    @Override
    public VirtualFilePointer[] getExcludeRootsForModule(ModuleRootLayer rootModel) {
        return VirtualFilePointer.EMPTY_ARRAY;
    }
}
