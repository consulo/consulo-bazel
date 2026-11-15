package org.jetbrains.bazel.utils;

import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public final class AnActionEventUtils {
    private AnActionEventUtils() {
    }

    public static @Nullable VirtualFile getSelectedDirectory(AnActionEvent receiver) {
        VirtualFile file = receiver.getData(VirtualFile.KEY);
        return file != null && file.isDirectory() ? file : null;
    }
}
