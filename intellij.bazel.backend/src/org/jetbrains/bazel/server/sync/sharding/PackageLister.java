/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.server.sync.sharding;

import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.commons.symlinks.BazelConvenienceSymlinksScanner;
import org.jetbrains.bazel.label.Label;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Traverses Bazel packages specified by wildcard target patterns, expanding to a set of
 * single-package target patterns.
 */
public final class PackageLister {
    private PackageLister() {
    }

    /**
     * Expands all-in-package-recursive wildcard targets into all-in-single-package targets by
     * traversing the file system, looking for child bazel packages.
     */
    public static Map<Label, List<Label>> expandPackageTargets(
        BazelPathsResolver pathResolver,
        List<Label> wildcardPatterns
    ) {
        Set<Path> calculatedBazelSymlinks = new BazelConvenienceSymlinksScanner(pathResolver.workspaceRoot()).scan();
        Map<Label, List<Label>> result = new LinkedHashMap<>();
        for (Label pattern : wildcardPatterns) {
            if (!pattern.isRecursive()) {
                continue;
            }
            Path dir = pathResolver.relativePathToWorkspaceAbsolute(Label.toPath(pattern.packagePath()));
            if (!Files.isDirectory(dir)) {
                continue;
            }

            List<Label> expandedTargets = new ArrayList<>();
            List<Path> dirs = new ArrayList<>();
            dirs.add(dir);
            traversePackageRecursively(pathResolver, calculatedBazelSymlinks, dirs, expandedTargets);
            result.put(pattern, expandedTargets);
        }
        return result;
    }

    // consulo: upstream tailrec function, the tail recursion is a loop
    private static void traversePackageRecursively(
        BazelPathsResolver pathResolver,
        Set<Path> calculatedBazelSymlinks,
        List<Path> dirs,
        List<Label> output
    ) {
        while (true) {
            if (dirs.isEmpty()) {
                return;
            }
            Path dir = dirs.remove(0);
            if (isEligibleForTraversal(dir, pathResolver, calculatedBazelSymlinks)) {
                String path = pathResolver.getWorkspaceRelativePath(dir);
                if (containsBuildFile(dir)) {
                    output.add(Label.parse("//" + path + ":all"));
                }
                File[] listFiles = dir.toFile().listFiles();
                File[] children = listFiles != null ? listFiles : new File[0];
                Arrays.stream(children).filter(File::isDirectory).map(File::toPath).forEach(dirs::add);
            }
        }
    }

    private static boolean isEligibleForTraversal(Path receiver, BazelPathsResolver pathResolver, Set<Path> calculatedBazelSymlinks) {
        return !isDotBazelBsp(receiver, pathResolver) && !calculatedBazelSymlinks.contains(receiver);
    }

    private static boolean isDotBazelBsp(Path receiver, BazelPathsResolver pathResolver) {
        Path fileName = receiver.getFileName();
        return fileName != null && fileName.toString().equals(Constants.DOT_BAZELBSP_DIR_NAME)
            && Objects.equals(receiver.getParent(), pathResolver.workspaceRoot());
    }

    private static boolean containsBuildFile(Path receiver) {
        return Arrays.stream(Constants.BUILD_FILE_NAMES).anyMatch(it -> Files.isRegularFile(receiver.resolve(it)));
    }
}
