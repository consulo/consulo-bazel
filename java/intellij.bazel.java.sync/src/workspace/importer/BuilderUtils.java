package org.jetbrains.bazel.workspace.importer;

import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.nio.file.Path;

// consulo: upstream Kotlin top-level functions; VirtualFileUrl (workspace model) is a VFS url string
public final class BuilderUtils {
    private BuilderUtils() {
    }

    public static String toResolvedVirtualFileUrl(Path receiver) {
        String url = VirtualFileUtil.pathToUrl(receiver.toString());
        // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
        VirtualFileManager.getInstance().findFileByUrl(url);
        return url;
    }

    public static String toResolvedVirtualFileUrl(String receiver) {
        String url = receiver;
        // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
        VirtualFileManager.getInstance().findFileByUrl(url);
        return url;
    }

    public static String toJarUrlString(Path receiver) {
        if ("jar".equals(extension(receiver))) {
            return "jar://" + receiver + "!/";
        }
        else {
            // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
            return "file://" + receiver;
        }
    }

    // consulo: upstream `toVirtualFileUrl(virtualFileUrlManager)` without resolving the virtual file
    public static String toVirtualFileUrl(Path receiver) {
        return VirtualFileUtil.pathToUrl(receiver.toString());
    }

    // consulo: kotlin.io.path.extension
    public static String extension(Path receiver) {
        Path fileName = receiver.getFileName();
        String name = fileName == null ? "" : fileName.toString();
        int index = name.lastIndexOf('.');
        return index == -1 ? "" : name.substring(index + 1);
    }
}
