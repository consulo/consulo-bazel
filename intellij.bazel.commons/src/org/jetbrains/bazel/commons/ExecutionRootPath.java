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

import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

/**
 * An absolute or relative path returned from Bazel. If it is a relative path, it is relative to the
 * execution root.
 */
public record ExecutionRootPath(Path absoluteOrRelativePath) {
    public ExecutionRootPath(String path) {
        this(Path.of(path));
    }

    public boolean isAbsolute() {
        return absoluteOrRelativePath.isAbsolute();
    }

    public Path getPathRootedAt(Path absoluteRoot) {
        if (absoluteOrRelativePath.isAbsolute()) {
            return absoluteOrRelativePath;
        }
        else {
            return absoluteRoot.resolve(absoluteOrRelativePath);
        }
    }

    /**
     * Returns the relative {@link ExecutionRootPath} if `root` is an ancestor of `path`
     * otherwise returns null.
     */
    public static @Nullable ExecutionRootPath createAncestorRelativePath(Path root, Path path) {
        // We cannot find the relative path between an absolute and relative path
        if (root.isAbsolute() != path.isAbsolute()) {
            return null;
        }

        Path rootAbs = root.toAbsolutePath();
        Path pathAbs = path.toAbsolutePath();

        if (!isAncestor(rootAbs.toString(), pathAbs.toString(), false)) {
            return null;
        }

        Path relativePath = rootAbs.relativize(pathAbs);
        return new ExecutionRootPath(relativePath);
    }

    public static @Nullable ExecutionRootPath createAncestorRelativePath(File root, File path) {
        return createAncestorRelativePath(root.toPath(), path.toPath());
    }

    /**
     * @param possibleParent
     * @param possibleChild
     * @param strict         if `false` then this method returns `true` if `possibleParent`
     *                       equals to `possibleChild`.
     */
    public static boolean isAncestor(
        ExecutionRootPath possibleParent,
        ExecutionRootPath possibleChild,
        boolean strict
    ) {
        return isAncestor(
            possibleParent.absoluteOrRelativePath().toString(),
            possibleChild.absoluteOrRelativePath().toString(),
            strict
        );
    }

    /**
     * @param possibleParentPath
     * @param possibleChild
     * @param strict             if `false` then this method returns `true` if `possibleParent`
     *                           equals to `possibleChild`.
     */
    public static boolean isAncestor(
        String possibleParentPath,
        ExecutionRootPath possibleChild,
        boolean strict
    ) {
        return isAncestor(
            possibleParentPath,
            possibleChild.absoluteOrRelativePath().toString(),
            strict
        );
    }

    /**
     * @param possibleParent
     * @param possibleChildPath
     * @param strict            if `false` then this method returns `true` if `possibleParent`
     *                          equals to `possibleChild`.
     */
    public static boolean isAncestor(
        ExecutionRootPath possibleParent,
        String possibleChildPath,
        boolean strict
    ) {
        return isAncestor(
            possibleParent.absoluteOrRelativePath().toString(),
            possibleChildPath,
            strict
        );
    }

    /**
     * @param possibleParentPath
     * @param possibleChildPath
     * @param strict             if `false` then this method returns `true` if `possibleParent`
     *                           equals to `possibleChild`.
     */
    public static boolean isAncestor(
        String possibleParentPath,
        String possibleChildPath,
        boolean strict
    ) {
        return FileUtil.isAncestor(possibleParentPath, possibleChildPath, strict);
    }
}
