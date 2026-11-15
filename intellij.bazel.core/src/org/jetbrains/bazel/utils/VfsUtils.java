package org.jetbrains.bazel.utils;

import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class VfsUtils {
    private VfsUtils() {
    }

    public static @Nullable VirtualFile findNearestParent(VirtualFile receiver, Set<VirtualFile> parents) {
        VirtualFile parent = receiver;
        while (true) {
            if (parents.contains(parent)) {
                return parent;
            }
            parent = parent.getParent();
            if (parent == null) {
                return null;
            }
        }
    }
}
