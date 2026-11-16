// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.bazel.languages.starlark.globbing;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Implementation of a subset of UNIX-style file globbing, expanding "*" and "?" as wildcards, but
 * not [a-z] ranges.
 * <p>
 * {@code **} gets special treatment in include patterns. If it is used as a complete path
 * segment it matches the filenames in subdirectories recursively.
 * <p>
 * Largely copied from old Bazel plugin's UnixGlob.java
 */
public class StarlarkGlob {
    private final VirtualFile myBase;
    private final List<List<String>> myPatterns;
    private final List<List<String>> myExcludes;
    private final boolean myExcludeDirectories;
    private final Predicate<VirtualFile> myPathFilter;

    private StarlarkGlob(
        VirtualFile base,
        List<List<String>> patterns,
        List<List<String>> excludes,
        boolean excludeDirectories,
        Predicate<VirtualFile> pathFilter
    ) {
        myBase = base;
        myPatterns = patterns;
        myExcludes = excludes;
        myExcludeDirectories = excludeDirectories;
        myPathFilter = pathFilter;
    }

    private final ConcurrentHashMap<String, Pattern> myCache = createCache();

    /**
     * Checks if given file matches the glob pattern.
     */
    public boolean match(VirtualFile file) {
        if (file.isDirectory() && myExcludeDirectories) {
            return false;
        }
        String relativePath = VirtualFileUtil.getRelativePath(file, myBase);
        if (relativePath == null) {
            return false; // relative path is null when file is not under base
        }

        VirtualFile subDir = myBase;
        List<String> segments = Arrays.asList(relativePath.split("/", -1));
        for (String segment : segments.subList(0, Math.max(segments.size() - 1, 0))) {
            subDir = subDir.findChild(segment);
            if (subDir == null) {
                return false;
            }
            if (!myPathFilter.test(subDir)) {
                return false;
            }
        }

        return match(relativePath);
    }

    /**
     * Checks if the given relative path matches the glob pattern.
     */
    public boolean match(String relativePath) {
        boolean included = myPatterns.stream().anyMatch(include -> patternMatches(include, relativePath, myExcludeDirectories, myCache));
        if (included) {
            boolean excluded =
                myExcludes.stream().anyMatch(exclude -> patternMatches(exclude, relativePath, myExcludeDirectories, myCache));
            return !excluded;
        }
        return false;
    }

    /**
     * Executes the glob.
     */
    public List<VirtualFile> execute() {
        if (!myBase.exists() || myPatterns.isEmpty()) {
            return List.of();
        }

        // If called on EDT, use modal progress to avoid freezing the UI completely.
        if (Application.get().isDispatchThread()) {
            Project project = ProjectLocator.getInstance().guessProjectForFile(myBase);
            // consulo: runWithModalProgressBlocking(ModalTaskOwner) -> runProcessWithProgressSynchronously (null project = guessed owner)
            ThrowableSupplier<List<VirtualFile>, RuntimeException> process = this::executeSuspend;
            return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                process,
                StarlarkLocalize.progressGlobbing(),
                true,
                project
            );
        }

        // consulo: runBlockingCancellable -> direct call, cancellation goes through the current progress indicator
        return executeSuspend();
    }

    // consulo: upstream suspend function; it runs synchronously, callers provide the progress
    public List<VirtualFile> executeSuspend() {
        if (!myBase.exists() || myPatterns.isEmpty()) {
            return List.of();
        }
        ProgressManager.checkCanceled();

        Set<VirtualFile> included = new GlobVisitor(myExcludeDirectories, myPathFilter)
            .globAsync(myBase, myPatterns);
        if (included.isEmpty()) {
            return List.of();
        }

        Set<VirtualFile> excluded = new GlobVisitor(myExcludeDirectories, myPathFilter)
            .globAsync(myBase, myExcludes);

        Set<VirtualFile> result = new LinkedHashSet<>(included);
        result.removeAll(excluded);
        List<VirtualFile> sorted = new ArrayList<>(result);
        sorted.sort(Comparator.comparing(VirtualFile::getPath));
        return sorted;
    }

    /**
     * GlobVisitor executes a glob using parallelism, which is useful when the glob() requires many
     * readdir() calls on high latency filesystems.
     */
    private class GlobVisitor {
        private final boolean myExcludeDirectories;
        private final Predicate<VirtualFile> myDirPred;

        private final Set<VirtualFile> myResults = ConcurrentHashMap.newKeySet();

        private GlobVisitor(boolean excludeDirectories, Predicate<VirtualFile> dirPred) {
            myExcludeDirectories = excludeDirectories;
            myDirPred = dirPred;
        }

        /**
         * Performs wildcard globbing: returns the sorted list of filenames that match any of {@code patterns} relative to {@code base}.
         * Directories are traversed if and only if they match
         * {@code dirPred}. The predicate is also called for the root of the traversal.
         * <p>
         * Patterns may include "*" and "?", but not "[a-z]".
         * <p>
         * {@code **} gets special treatment in include patterns. If it is used as a complete
         * path segment it matches the filenames in subdirectories recursively.
         *
         * @throws IllegalArgumentException if any glob or exclude pattern contains errors, or if any
         *                                  exclude pattern segment contains {@code **} or if any include pattern segment
         *                                  contains {@code **} but not equal to it.
         */
        // consulo: upstream suspend function; coroutine launch() calls are replaced by direct recursive calls
        // (results are collected into a set and sorted afterwards, so the visiting order does not matter)
        Set<VirtualFile> globAsync(
            VirtualFile base,
            Collection<List<String>> patterns
        ) {
            boolean baseIsDirectory = base.isDirectory();

            // We do a dumb loop, even though it will likely duplicate work
            // (e.g., readdir calls). In order to optimize, we would need
            // to keep track of which patterns shared sub-patterns and which did not
            // (for example consider the glob [*/*.java, sub/*.java, */*.txt]).
            for (List<String> splitPattern : patterns) {
                reallyGlob(
                    base,
                    baseIsDirectory,
                    splitPattern,
                    0
                );
            }
            return myResults;
        }

        /**
         * Expressed in Haskell:
         *
         * <pre>
         * reallyGlob base []     = { base }
         * reallyGlob base [x:xs] = union { reallyGlob(f, xs) | f results "base/x" }
         * </pre>
         */
        void reallyGlob(
            VirtualFile base,
            boolean baseIsDirectory,
            List<String> patternParts,
            int idx
        ) {
            ProgressManager.checkCanceled();
            if (baseIsDirectory && !myDirPred.test(base)) {
                return;
            }

            if (idx == patternParts.size()) { // Base case.
                if (!(myExcludeDirectories && baseIsDirectory)) {
                    myResults.add(base);
                }
                return;
            }

            if (!baseIsDirectory) {
                // Nothing to find here.
                return;
            }

            String pattern = patternParts.get(idx);

            // ** is special: it can match nothing at all.
            // For example, x/** matches x, **/y matches y, and x/**/y matches x/y.
            if ("**".equals(pattern)) {
                reallyGlob(
                    base,
                    baseIsDirectory,
                    patternParts,
                    idx + 1
                );
            }

            if (!pattern.contains("*") && !pattern.contains("?")) {
                ProgressManager.checkCanceled();
                VirtualFile child = base.findChild(pattern);
                if (child == null) {
                    return;
                }
                boolean childIsDir = child.isDirectory();
                // consulo: Kotlin VirtualFile.isFile
                boolean childIsFile = child.isValid() && !child.isDirectory();
                if (!childIsDir && !childIsFile) {
                    // The file is a dangling symlink, fifo, does not exist, etc.
                    return;
                }
                reallyGlob(
                    child,
                    childIsDir,
                    patternParts,
                    idx + 1
                );
                return;
            }

            VirtualFile[] children = getChildren(base);
            if (children == null) {
                return;
            }

            for (VirtualFile child : children) {
                ProgressManager.checkCanceled();

                String childName = child.getName();
                boolean childIsDir = child.isDirectory();

                if ("**".equals(pattern) && childIsDir) {
                    // Recurse without shifting the pattern.
                    reallyGlob(
                        child,
                        childIsDir,
                        patternParts,
                        idx
                    );
                }
                if (partMatches(pattern, childName, myCache)) {
                    // Recurse and consume one segment of the pattern.
                    if (childIsDir) {
                        reallyGlob(
                            child,
                            childIsDir,
                            patternParts,
                            idx + 1
                        );
                    }
                    else {
                        // Instead of using an async call, just repeat the base case above.
                        if (idx + 1 == patternParts.size()) {
                            myResults.add(child);
                        }
                    }
                }
            }
        }

        VirtualFile @Nullable [] getChildren(@Nullable VirtualFile file) {
            return file == null ? null : file.getChildren();
        }
    }

    /**
     * Builder class for UnixGlob.
     */
    public static class Builder {
        private final VirtualFile myBase;
        private final List<String> myPatterns = new ArrayList<>();
        private final List<String> myExcludes = new ArrayList<>();
        private boolean myExcludeDirectories = true;
        private Predicate<VirtualFile> myPathFilter = file -> true;

        public Builder(VirtualFile base) {
            myBase = base;
        }

        /**
         * Adds a pattern to include to the glob builder.
         * <p>
         * For a description of the syntax of the patterns, see {@link StarlarkGlob}.
         */
        @SuppressWarnings("unused")
        public Builder addPattern(String pattern) {
            myPatterns.add(pattern);
            return this;
        }

        /**
         * Adds a pattern to include to the glob builder.
         * <p>
         * For a description of the syntax of the patterns, see {@link StarlarkGlob}.
         */
        public Builder addPatterns(Collection<String> patterns) {
            myPatterns.addAll(patterns);
            return this;
        }

        /**
         * Adds patterns to exclude from the results to the glob builder.
         * <p>
         * For a description of the syntax of the patterns, see {@link StarlarkGlob}.
         */
        public Builder addExcludes(Collection<String> excludes) {
            myExcludes.addAll(excludes);
            return this;
        }

        /**
         * If set to true, directories are not returned in the glob result.
         */
        public Builder setExcludeDirectories(boolean excludeDirectories) {
            myExcludeDirectories = excludeDirectories;
            return this;
        }

        /**
         * If set, the given predicate is called for every directory encountered. If it returns false,
         * the corresponding item is not returned in the output and directories are not traversed
         * either.
         * <p>
         * The predicate must be pure and fast, and it must not access PSI, indexes or other APIs
         * that require a read action. It should only use VFS-level information available from
         * {@link VirtualFile}.
         */
        public Builder setDirectoryFilter(Predicate<VirtualFile> pathFilter) {
            myPathFilter = pathFilter;
            return this;
        }

        /**
         * Construct glob of this builder
         */
        public StarlarkGlob build() {
            return new StarlarkGlob(myBase, checkAndSplitPatterns(myPatterns), checkAndSplitPatterns(myExcludes), myExcludeDirectories,
                myPathFilter);
        }
    }

    /**
     * Checks that each pattern is valid, splits it into segments and checks that each segment
     * contains only valid wildcards.
     *
     * @return list of segment arrays
     */
    private static List<List<String>> checkAndSplitPatterns(Collection<String> patterns) {
        List<List<String>> result = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            result.add(checkAndSplitPattern(pattern));
        }
        return result;
    }

    private static List<String> checkAndSplitPattern(String pattern) {
        String error = GlobPatternValidator.validate(pattern);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        return List.of(pattern.split("/", -1));
    }

    /**
     * Calls {@link #partMatches(String, String, ConcurrentHashMap) matches(pattern, str, null)}
     */
    public static boolean partMatches(String pattern, String str) {
        return partMatches(pattern, str, null);
    }

    public static boolean patternMatches(
        List<String> patternParts,
        String path,
        boolean excludeDirectories,
        @Nullable ConcurrentHashMap<String, Pattern> patternCache
    ) {
        int slashIndex = path.indexOf('/');
        String pathPart = slashIndex < 0 ? path : path.substring(0, slashIndex);
        String pathTail = slashIndex < 0 ? "" : path.substring(slashIndex + 1);
        boolean isDirectory = !pathTail.isEmpty();

        if (patternParts.isEmpty()) { // Base case.
            if (!(excludeDirectories && isDirectory)) {
                return true;
            }
            return false;
        }

        //if (!isDirectory) {
        //  // Nothing to find here.
        //  return false
        //}

        String pattern = patternParts.get(0);

        // ** is special: it can match nothing at all.
        // For example, x/** matches x, **/y matches y, and x/**/y matches x/y.
        if ("**".equals(pattern)) {
            if (patternMatches(tail(patternParts), path, excludeDirectories, patternCache)) {
                return true;
            }
        }

        if (!pattern.contains("*") && !pattern.contains("?")) {
            if (!pathPart.equals(pattern)) {
                return false;
            }

            patternMatches(tail(patternParts), pathTail, excludeDirectories, patternCache);
        }

        if ("**".equals(pattern) && isDirectory) {
            // Recurse without shifting the pattern.
            if (patternMatches(patternParts, pathTail, excludeDirectories, patternCache)) {
                return true;
            }
        }

        if (partMatches(pattern, pathPart, patternCache)) {
            // Recurse and consume one segment of the pattern.
            if (isDirectory) {
                if (patternMatches(tail(patternParts), pathTail, excludeDirectories, patternCache)) {
                    return true;
                }
            }
            else {
                if (patternParts.size() == 1) {
                    return true;
                }
            }
        }

        return false;
    }

    // consulo: com.intellij.util.containers.tail
    private static List<String> tail(List<String> list) {
        return list.subList(1, list.size());
    }

    /**
     * Returns whether {@code str} matches the glob pattern {@code pattern}. This method may use the
     * {@code patternCache} to speed up the matching process.
     *
     * @param pattern      a glob pattern
     * @param str          the string to match
     * @param patternCache a cache from patterns to compiled Pattern objects, or {@code null} to skip
     *                     caching
     */
    private static boolean partMatches(
        String pattern,
        String str,
        @Nullable ConcurrentHashMap<String, Pattern> patternCache
    ) {
        if (pattern.isEmpty() || str.isEmpty()) {
            return false;
        }

        // Common case: **
        if (pattern.equals("**")) {
            return true;
        }

        // Common case: *
        if (pattern.equals("*")) {
            return true;
        }

        // If a filename starts with '.', this char must be matched explicitly.
        if (str.charAt(0) == '.' && pattern.charAt(0) != '.') {
            return false;
        }

        // Common case: *.xyz
        if (pattern.charAt(0) == '*' && pattern.lastIndexOf('*') == 0) {
            return str.endsWith(pattern.substring(1));
        }
        // Common case: xyz*!!
        int lastIndex = pattern.length() - 1;
        // The first clause of this if statement is unnecessary, but is an
        // optimization--charAt runs faster than indexOf.!!!!
        if (pattern.charAt(lastIndex) == '*' && pattern.indexOf('*') == lastIndex) {
            return str.startsWith(pattern.substring(0, lastIndex));
        }

        Pattern regex = patternCache != null
            ? patternCache.computeIfAbsent(pattern, StarlarkGlob::makePatternFromWildcard)
            : makePatternFromWildcard(pattern);
        return regex.matcher(str).matches();
    }

    /**
     * Returns a regular expression implementing a matcher for "pattern", in which "*" and "?" are
     * wildcards.
     * <p>
     * e.g. "foo*bar?.java" -> "foo.*bar.\\.java"
     */
    private static Pattern makePatternFromWildcard(String pattern) {
        StringBuilder regexp = new StringBuilder();
        int i = 0;
        int len = pattern.length();
        while (i < len) {
            char c = pattern.charAt(i);
            switch (c) {
                case '*' -> {
                    int toIncrement = 0;
                    if (len > i + 1 && pattern.charAt(i + 1) == '*') {
                        // The pattern '**' is interpreted to match 0 or more directory separators, not 1 or
                        // more. We skip the next * and then find a trailing/leading '/' and get rid of it.
                        toIncrement = 1;
                        if (len > i + 2 && pattern.charAt(i + 2) == '/') {
                            // We have '**/' -- skip the '/'.
                            toIncrement = 2;
                        }
                        else if (len == i + 2 && i > 0 && pattern.charAt(i - 1) == '/') {
                            // We have '/**' -- remove the '/'.
                            regexp.delete(regexp.length() - 1, regexp.length());
                        }
                    }
                    regexp.append(".*");
                    i += toIncrement;
                }

                case '?' -> regexp.append('.');
                case '^', '$', '|', '+', '{', '}', '[', ']', '\\', '.' -> {
                    regexp.append('\\');
                    regexp.append(c);
                }

                default -> regexp.append(c);
            }
            i++;
        }
        return Pattern.compile(regexp.toString());
    }

    public static Builder forPath(VirtualFile path) {
        return new Builder(path);
    }

    public static ConcurrentHashMap<String, Pattern> createCache() {
        return new ConcurrentHashMap<>();
    }
}
