package org.jetbrains.bazel.workspace;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import org.jetbrains.bazel.utils.PathUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface ProjectViewGlobSet {

    /**
     * Returns true if the path matches the glob set.
     */
    boolean matches(Path path);

    /**
     * Returns true if any pattern implies that all files under the {@code directory} are selected.
     * The caller then does not need to visit the directory.
     * <p>
     * It guarantees that the patterns match the whole subtree under {@code directory}.
     * It uses only the patterns and does not read the disk state.
     * A pattern can match all the files that are under {@code directory} now, but not all the possible files.
     * In this case, this function returns false.
     * For example, the `foo` directory holds only Java files.
     * This function returns `false` for the foo&#47;&#42;.java pattern and `true` for the foo&#47;&#42; pattern.
     */
    boolean impliesRecursiveMatch(Path directory);

    ProjectViewGlobSet EMPTY = new ProjectViewGlobSet() {
        @Override
        public boolean matches(Path path) {
            return false;
        }

        @Override
        public boolean impliesRecursiveMatch(Path directory) {
            return false;
        }
    };

    static ProjectViewGlobSet of(Path root, String... patterns) {
        return of(root, Arrays.asList(patterns));
    }

    static ProjectViewGlobSet of(Path root, List<String> patterns) {
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("Failed requirement.");
        }
        if (patterns.isEmpty()) {
            return EMPTY;
        }
        if (patterns.stream().anyMatch(it -> it.equals("*") || it.equals("**"))) {
            return new MatchAllProjectViewGlobSet(root);
        }
        return impl(root, patterns);
    }

    private static ProjectViewGlobSet impl(Path root, List<String> patterns) {
        FileNameMatcherFactory factory = FileNameMatcherFactory.getInstance();
        Set<String> filenames = new LinkedHashSet<>();
        Set<String> extensions = new LinkedHashSet<>();
        Set<Path> directories = new LinkedHashSet<>();
        List<FileNameMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            // regular filename
            if (!pattern.contains("/") && !containsWildcards(pattern)) {
                filenames.add(pattern);
                continue;
            }
            // just *.extension pattern
            if (!pattern.contains("/") && pattern.startsWith("*.") && !containsWildcards(pattern, 2)) {
                extensions.add(pattern.substring(2));
                continue;
            }
            // just a dir/* or dir/
            if (isRecursiveDirectoryPattern(pattern)) {
                directories.add(Path.of(StringUtil.trimTrailing(pattern, '*')));
                continue;
            }
            // See https://github.com/bazelbuild/intellij/blob/e7aa7f57260ac473cfa1b072b01400f81eed925d/base/src/com/google/idea/blaze/base/sync/projectview/SourceTestConfig.java#L44
            String withAsteriskAtEnd =
                StringUtil.trimTrailing(StringUtil.trimTrailing(pattern, '*'), '/') + '*';
            matchers.add(factory.createMatcher(withAsteriskAtEnd));
        }
        return new ProjectViewGlobSetImpl(root, filenames, directories, extensions, matchers);
    }

    private static boolean isRecursiveDirectoryPattern(String receiver) {
        String prefix = StringUtil.trimTrailing(receiver, '*');
        return prefix.endsWith("/") && !containsWildcards(prefix);
    }

    private static boolean containsWildcards(String receiver) {
        return containsWildcards(receiver, 0);
    }

    private static boolean containsWildcards(String receiver, int startIndex) {
        // consulo: Kotlin indexOfAny("*?".toCharArray(), startIndex) != -1
        for (int i = Math.max(startIndex, 0); i < receiver.length(); i++) {
            char c = receiver.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
        }
        return false;
    }
}

class MatchAllProjectViewGlobSet implements ProjectViewGlobSet {
    private final Set<Path> myAncestors;

    MatchAllProjectViewGlobSet(Path root) {
        myAncestors = Set.of(root);
    }

    @Override
    public boolean matches(Path path) {
        return !path.isAbsolute() || PathUtil.isUnder(path, myAncestors);
    }

    @Override
    public boolean impliesRecursiveMatch(Path directory) {
        return matches(directory);
    }
}

class ProjectViewGlobSetImpl implements ProjectViewGlobSet {
    private final Path myRootDir;
    private final Set<String> myFilenames;
    private final Set<Path> myDirectories;
    private final Set<String> myExtensions;
    private final List<FileNameMatcher> myMatchers;

    ProjectViewGlobSetImpl(
        Path rootDir,
        Set<String> filenames,
        Set<Path> directories,
        Set<String> extensions,
        List<FileNameMatcher> matchers
    ) {
        myRootDir = rootDir;
        myFilenames = filenames;
        myDirectories = directories;
        myExtensions = extensions;
        myMatchers = matchers;
    }

    @Override
    public boolean matches(Path path) {
        Path relativeFromWorkspaceRoot = relativeFromWorkspaceRoot(path);
        if (relativeFromWorkspaceRoot == null) {
            return false;
        }
        if (PathUtil.isUnder(relativeFromWorkspaceRoot, myDirectories)) {
            return true;
        }
        // consulo: Kotlin Path.invariantSeparatorsPathString
        String separator = relativeFromWorkspaceRoot.getFileSystem().getSeparator();
        return matchesPathString(relativeFromWorkspaceRoot.toString().replace(separator, "/"));
    }

    @Override
    public boolean impliesRecursiveMatch(Path directory) {
        Path relativeFromWorkspaceRoot = relativeFromWorkspaceRoot(directory);
        return relativeFromWorkspaceRoot != null && PathUtil.isUnder(relativeFromWorkspaceRoot, myDirectories);
    }

    private @Nullable Path relativeFromWorkspaceRoot(Path receiver) {
        if (!receiver.isAbsolute()) {
            return receiver;
        }
        // consulo: Kotlin Path.relativeToOrNull
        try {
            return myRootDir.normalize().relativize(receiver.normalize());
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean matchesPathString(String path) {
        // consulo: Kotlin substringAfterLast("/") and substringAfterLast('.', "")
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (myFilenames.contains(filename)) {
            return true;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (myExtensions.contains(dotIndex < 0 ? "" : filename.substring(dotIndex + 1))) {
            return true;
        }
        return myMatchers.stream().anyMatch(it -> it.acceptsCharSequence(path));
    }
}
