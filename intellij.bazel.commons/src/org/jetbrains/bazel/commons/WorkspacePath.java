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
package org.jetbrains.bazel.commons;

import consulo.platform.Platform;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a path relative to the workspace root. The path component separator is Bazel specific.
 * <p>
 * <p>
 * A {@link WorkspacePath} is *not* necessarily a valid package name/path. The primary reason is
 * because it could represent a file and files don't have to follow the same conventions as package
 * names.
 */
// consulo: data class with a private constructor, so not a record
public final class WorkspacePath {
    private final Path myPath;

    private WorkspacePath(Path path) {
        myPath = path;
    }

    /**
     * @param relativePath relative path that must use the Bazel specific separator char to separate
     *                     path components
     * @throws IllegalArgumentException if the path is invalid
     */
    public WorkspacePath(String relativePath) {
        this(validateAndCreatePath(relativePath));
    }

    public WorkspacePath(WorkspacePath parentPath, String childPath) {
        this(
            parentPath.isWorkspaceRoot()
                ? validateAndCreatePath(childPath)
                : validateAndCreatePath(parentPath.relativePath() + BAZEL_COMPONENT_SEPARATOR + childPath)
        );
    }

    /**
     * Returns the workspace path of this path's parent directory. Returns null if this is the
     * workspace root.
     */
    public @Nullable WorkspacePath getParent() {
        if (this.isWorkspaceRoot()) {
            return null;
        }
        Path parentPath = myPath.getParent();
        return parentPath != null ? new WorkspacePath(parentPath) : null;
    }

    /** Returns this workspace path, relative to the workspace root.  */
    public Path asPath() {
        return myPath;
    }

    public boolean isWorkspaceRoot() {
        return myPath.toString().isEmpty() || myPath.toString().equals(".");
    }

    @Override
    public String toString() {
        return myPath.toString();
    }

    public String relativePath() {
        return myPath.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof WorkspacePath that && myPath.equals(that.myPath);
    }

    @Override
    public int hashCode() {
        return myPath.hashCode();
    }

    /** Silently returns null if this is not a valid workspace path.  */
    public static @Nullable WorkspacePath createIfValid(String relativePath) {
        return isValid(relativePath) ? new WorkspacePath(Path.of(relativePath)) : null;
    }

    private static final char BAZEL_COMPONENT_SEPARATOR = '/';

    private static String normalizePathSeparator(String relativePath) {
        return Platform.current().os().isWindows() ? relativePath.replace('\\', BAZEL_COMPONENT_SEPARATOR) : relativePath;
    }

    private static Path validateAndCreatePath(String relativePath) {
        String normalizedPath = normalizePathSeparator(relativePath);
        String error = validate(normalizedPath);
        if (error != null) {
            throw new IllegalArgumentException("Invalid workspace path '" + relativePath + "': " + error);
        }
        return Paths.get(normalizedPath);
    }

    public static boolean isValid(String relativePath) {
        return validate(relativePath) == null;
    }

    /** Validates a workspace path. Returns null on success or an error message otherwise.  */
    public static @Nullable String validate(String relativePath) {
        if (relativePath.startsWith("/")) {
            return "Workspace path must be relative; cannot start with '/': " + relativePath;
        }
        if (relativePath.startsWith("../")) {
            return (
                "Workspace path must be inside the workspace; cannot start with '../': " +
                    relativePath
            );
        }
        if (relativePath.endsWith("/")) {
            return "Workspace path may not end with '/': " + relativePath;
        }

        if (relativePath.indexOf(':') >= 0) {
            return "Workspace path may not contain ':': " + relativePath;
        }

        return null;
    }
}
