/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.workspace;

import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.WorkspacePath;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

/**
 * Represents a workspace root
 */
public record WorkspaceRoot(Path directory) {
    public WorkspaceRoot(File directory) {
        this(directory.toPath());
    }

    public Path fileForPath(WorkspacePath workspacePath) {
        return directory.resolve(workspacePath.relativePath());
    }

    // consulo: upstream fun directory(): Path is the record accessor

    public @Nullable Path absolutePathFor(String workspaceRelativePath) {
        return directory.resolve(workspaceRelativePath);
    }

    public Path path() {
        return directory;
    }

    public WorkspacePath workspacePathFor(Path file) {
        return workspacePathFor(file.toString());
    }

    public WorkspacePath workspacePathFor(VirtualFile file) {
        return workspacePathFor(file.getPath());
    }

    public Path relativize(VirtualFile file) {
        return workspacePathFor(file).asPath();
    }

    public @Nullable Path tryRelativize(VirtualFile file) {
        if (!isInWorkspace(file)) {
            return null;
        }

        return relativize(file);
    }

    private WorkspacePath workspacePathFor(String path) {
        if (!isInWorkspace(path)) {
            throw new IllegalArgumentException(String.format("File '%s' is not under workspace %s", path, directory));
        }
        String dirPathStr = directory.toString();
        if (dirPathStr.length() == path.length()) {
            return new WorkspacePath("");
        }
        return new WorkspacePath(path.substring(dirPathStr.length() + 1));
    }

    /**
     * Returns the WorkspacePath for the given absolute file, if it's a child of this WorkspaceRoot
     * and a valid WorkspacePath. Otherwise returns null.
     */
    public @Nullable WorkspacePath workspacePathForSafe(Path absoluteFile) {
        return workspacePathForSafe(absoluteFile.toString());
    }

    public @Nullable WorkspacePath workspacePathForSafe(File absoluteFile) {
        return workspacePathForSafe(absoluteFile.toPath());
    }

    /**
     * Returns the WorkspacePath for the given virtual file, if it's a child of this WorkspaceRoot and
     * a valid WorkspacePath. Otherwise returns null.
     */
    public @Nullable WorkspacePath workspacePathForSafe(VirtualFile file) {
        return workspacePathForSafe(file.getPath());
    }

    private @Nullable WorkspacePath workspacePathForSafe(String path) {
        if (!isInWorkspace(path)) {
            return null;
        }
        String dirPathStr = directory.toString();
        if (dirPathStr.length() == path.length()) {
            return new WorkspacePath("");
        }
        return WorkspacePath.createIfValid(path.substring(dirPathStr.length() + 1));
    }

    public boolean isInWorkspace(Path file) {
        return file.startsWith(directory);
    }

    public boolean isInWorkspace(File file) {
        return isInWorkspace(file.toPath());
    }

    public boolean isInWorkspace(VirtualFile file) {
        return isInWorkspace(file.getPath());
    }

    private boolean isInWorkspace(String path) {
        return consulo.util.io.FileUtil
            .isAncestor(directory.toString(), path, false);
    }
}
