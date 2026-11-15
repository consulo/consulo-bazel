package org.jetbrains.bazel.clion.debug;

import consulo.annotation.component.ActionImpl;
import consulo.application.ReadAction;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.project.Project;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Reports every execution-root file that the VFS holds, plus a file-extension histogram.
 */
// consulo: the group Bazel.CLion.DebugActions (no class upstream) is written by the coordinator
@ActionImpl(id = "Bazel.CLion.DebugExcrootVFS")
public class DebugExcrootVFSAction extends BazelDebugAction {
    private static final String NO_EXTENSION = "<none>";

    public DebugExcrootVFSAction() {
        super(BazelCLionCoreLocalize.actionBazelClionDebugexcrootvfsText());
    }

    @Override
    protected CompletableFuture<?> exec(Project project) {
        WorkspaceSnapshot snapshot = WorkspaceHelpers.getWorkspaceSnapshotOrNull(project);
        Path root = snapshot == null ? fail("no execution root found") : snapshot.bazelInfo().execRoot();

        // consulo: readAction { } -> ReadAction.compute, exec runs on the background thread of the action
        return CompletableFuture.completedFuture(ReadAction.compute(() -> {
            VirtualFile virtualRoot = VirtualFileManager.getInstance().findFileByNioPath(root);
            if (virtualRoot == null) {
                return fail("no virtual file found for the execution root");
            }

            List<String> files = new ArrayList<>();
            Map<String, Long> histogram = new HashMap<>();

            persistedFilesUnder(virtualRoot, child -> {
                String extension = child.getExtension();
                if (extension == null) {
                    extension = NO_EXTENSION;
                }
                histogram.put(extension, histogram.getOrDefault(extension, 0L) + 1L);
                files.add(child.getPath());
            });

            Map<String, Long> sortedHistogram = new LinkedHashMap<>();
            histogram.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(it -> sortedHistogram.put(it.getKey(), it.getValue()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("execution_root", root.toString());
            result.put("file_count", files.size());
            result.put("histogram", sortedHistogram);
            result.put("files", files);
            return result;
        }));
    }

    /**
     * Walks the children that the VFS already holds.
     *
     * <p>The walk never loads a directory from the file system, so the output shows
     * the VFS state alone.
     */
    // consulo: Kotlin sequence { yield() } -> a consumer called for every file
    private static void persistedFilesUnder(VirtualFile dir, Consumer<VirtualFile> consumer) {
        if (!(dir instanceof NewVirtualFile newVirtualFile)) {
            return;
        }
        if (!ManagingFS.getInstance().wereChildrenAccessed(dir)) {
            return;
        }

        for (VirtualFile child : newVirtualFile.iterInDbChildren()) {
            if (child.isDirectory()) {
                persistedFilesUnder(child, consumer);
            }
            else {
                consumer.accept(child);
            }
        }
    }
}
