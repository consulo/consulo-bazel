package org.jetbrains.bazel.sync;

import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AllPackagesBeneath;
import org.jetbrains.bazel.label.Label.AllRuleTargets;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.Package;
import org.jetbrains.bazel.label.Label.PackageType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.label.Label.SingleTarget;
import org.jetbrains.bazel.label.Label.TargetType;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SyncFileClassifier {
    public record SyncFile(
        Path path,
        SyncFileKind kind,
        boolean isWorkspaceFile
    ) {
        // consulo: upstream default argument (isWorkspaceFile = true)
        public SyncFile(Path path, SyncFileKind kind) {
            this(path, kind, true);
        }
    }

    public sealed interface SyncFileKind {
        record Build(Label pattern) implements SyncFileKind {
        }

        // `pattern` matches every rule target of every package under the directory
        record Folder(Label pattern) implements SyncFileKind {
        }

        record Module(String canonicalRepoName) implements SyncFileKind {
        }

        final class Workspace implements SyncFileKind {
            public static final Workspace INSTANCE = new Workspace();

            private Workspace() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof Workspace;
            }

            @Override
            public int hashCode() {
                return Workspace.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "Workspace";
            }
        }

        final class ProjectView implements SyncFileKind {
            public static final ProjectView INSTANCE = new ProjectView();

            private ProjectView() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof ProjectView;
            }

            @Override
            public int hashCode() {
                return ProjectView.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "ProjectView";
            }
        }

        // `label` is file label, NOT owning target label - you need query for that
        record Source(Label label) implements SyncFileKind {
        }

        record Starlark(@Nullable Label label) implements SyncFileKind {
        }

        final class BazelDotFile implements SyncFileKind {
            public static final BazelDotFile INSTANCE = new BazelDotFile();

            private BazelDotFile() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof BazelDotFile;
            }

            @Override
            public int hashCode() {
                return BazelDotFile.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "BazelDotFile";
            }
        }

        final class External implements SyncFileKind {
            public static final External INSTANCE = new External();

            private External() {
            }

            // consulo: data object equality
            @Override
            public boolean equals(@Nullable Object o) {
                return o instanceof External;
            }

            @Override
            public int hashCode() {
                return External.class.getName().hashCode();
            }

            @Override
            public String toString() {
                return "External";
            }
        }
    }

    private static final String MAIN_CANONICAL_REPO_NAME = "";

    private static final Set<String> WORKSPACE_FILE_NAMES = workspaceFileNames();

    private static final Set<String> MODULE_FILE_NAMES =
        Set.of(Constants.MODULE_BAZEL_FILE_NAME, Constants.MODULE_BAZEL_LOCK_FILE_NAME, Constants.REPO_BAZEL_FILE_NAME);

    private static final Set<String> BAZEL_DOT_FILE_NAMES =
        Set.of(
            Constants.BAZEL_RC_FILE_NAME,
            Constants.BAZEL_IGNORE_FILE_NAME,
            Constants.BAZEL_VERSION_FILE_NAME,
            Constants.BAZELISK_RC_FILE_NAME
        );

    // consulo: upstream Constants.WORKSPACE_FILE_NAMES.toSet() - Constants.MODULE_BAZEL_FILE_NAME
    private static Set<String> workspaceFileNames() {
        Set<String> names = new LinkedHashSet<>(Arrays.asList(Constants.WORKSPACE_FILE_NAMES));
        names.remove(Constants.MODULE_BAZEL_FILE_NAME);
        return names;
    }

    private final RepoMapping myRepoMapping;
    private final BazelInfo myBazelInfo;

    private final Map<String, Path> myLocalRepositories;

    public SyncFileClassifier(RepoMapping repoMapping, BazelInfo bazelInfo) {
        myRepoMapping = repoMapping;
        myBazelInfo = bazelInfo;

        Map<String, Path> localRepositories = new LinkedHashMap<>();
        if (repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping) {
            for (Map.Entry<String, Path> entry : bzlmodRepoMapping.getCanonicalRepoNameToPath().entrySet()) {
                String canonicalRepoName = entry.getKey();
                if (!bzlmodRepoMapping.getNonLocalCanonicalRepoNames().contains(canonicalRepoName)) {
                    localRepositories.put(canonicalRepoName, bazelInfo.workspaceRoot().resolve(entry.getValue()).normalize());
                }
            }
        }
        // consulo: upstream RepoMappingDisabled -> mapOf()
        localRepositories.put(MAIN_CANONICAL_REPO_NAME, bazelInfo.workspaceRoot().normalize());
        myLocalRepositories = localRepositories;
    }

    public SyncFile classify(Path path) {
        EnclosingRepo repo = findEnclosingRepo(path);
        if (repo == null) {
            return new SyncFile(path, SyncFileKind.External.INSTANCE, false);
        }
        // consulo: Kotlin Path.name
        Path fileName = path.getFileName();
        String name = fileName != null ? fileName.toString() : "";
        // consulo: Kotlin Path.extension
        int dotIndex = name.lastIndexOf('.');
        String extension = dotIndex < 0 ? "" : name.substring(dotIndex + 1);
        SyncFileKind kind;
        // directory, every rule target under it is in the scope.
        if (Files.isDirectory(path)) {
            kind = new SyncFileKind.Folder(repo.folderLabelOf(path));
        }
        // BUILD, BUILD.bazel. The directory of the file is the package.
        else if (Arrays.asList(Constants.BUILD_FILE_NAMES).contains(name)) {
            kind = new SyncFileKind.Build(repo.labelOf(Objects.requireNonNull(path.getParent()), AllRuleTargets.INSTANCE));
        }
        // MODULE.bazel, MODULE.bazel.lock, REPO.bazel
        else if (MODULE_FILE_NAMES.contains(name)) {
            kind = new SyncFileKind.Module(repo.myCanonicalRepoName);
        }
        // WORKSPACE, WORKSPACE.bazel, WORKSPACE.bzlmod
        else if (WORKSPACE_FILE_NAMES.contains(name)) {
            kind = SyncFileKind.Workspace.INSTANCE;
        }
        // .bazelproject
        else if (extension.equals(Constants.PROJECT_VIEW_FILE_EXTENSION)) {
            kind = SyncFileKind.ProjectView.INSTANCE;
        }
        // .bazelrc, .bazelignore, .bazelversion, .bazeliskrc
        else if (BAZEL_DOT_FILE_NAMES.contains(name)) {
            kind = SyncFileKind.BazelDotFile.INSTANCE;
        }
        // .bzl
        else if (extension.equals("bzl")) {
            kind = new SyncFileKind.Starlark(repo.sourceLabelOf(path));
        }
        // normal source file
        else {
            ResolvedLabel label = repo.sourceLabelOf(path);
            kind = label != null ? new SyncFileKind.Source(label) : SyncFileKind.External.INSTANCE;
        }
        return new SyncFile(path, kind);
    }

    private @Nullable EnclosingRepo findEnclosingRepo(Path path) {
        // TODO: profile and optimize, for large number of local repositories can get slow
        Map.Entry<String, Path> found = null;
        for (Map.Entry<String, Path> entry : myLocalRepositories.entrySet()) {
            if (!path.startsWith(entry.getValue())) {
                continue;
            }
            // find deepest innermost repository
            if (found == null || entry.getValue().getNameCount() > found.getValue().getNameCount()) {
                found = entry;
            }
        }
        if (found == null) {
            return null;
        }
        return new EnclosingRepo(found.getValue(), found.getKey());
    }

    private static class EnclosingRepo {
        private final Path myPath;
        private final String myCanonicalRepoName;

        EnclosingRepo(Path path, String canonicalRepoName) {
            myPath = path;
            myCanonicalRepoName = canonicalRepoName;
        }

        @Nullable ResolvedLabel sourceLabelOf(Path file) {
            Path packageDirectory = findPackageDirectory(file.getParent());
            if (packageDirectory == null) {
                return null;
            }
            Path targetName = relativeToOrNull(file, packageDirectory);
            if (targetName == null) {
                return null;
            }
            // consulo: Kotlin Path.invariantSeparatorsPathString
            String invariantTargetName = targetName.toString().replace(targetName.getFileSystem().getSeparator(), "/");
            return labelOf(packageDirectory, new SingleTarget(invariantTargetName));
        }

        ResolvedLabel labelOf(Path packageDirectory, TargetType target) {
            return labelOf(new Package(relativeSegmentsOf(packageDirectory)), target);
        }

        ResolvedLabel folderLabelOf(Path directory) {
            return labelOf(new AllPackagesBeneath(relativeSegmentsOf(directory)), AllRuleTargets.INSTANCE);
        }

        private ResolvedLabel labelOf(PackageType packagePath, TargetType target) {
            return new ResolvedLabel(
                Canonical.createCanonicalOrMain(myCanonicalRepoName),
                packagePath,
                target
            );
        }

        private List<String> relativeSegmentsOf(Path directory) {
            Path relative = relativeToOrNull(directory, myPath);
            return relative != null ? segments(relative) : List.of();
        }

        // TODO: cache that?
        private @Nullable Path findPackageDirectory(@Nullable Path directory) {
            Path candidate = directory;
            while (candidate != null && candidate.startsWith(myPath)) {
                for (String buildFileName : Constants.BUILD_FILE_NAMES) {
                    if (Files.isRegularFile(candidate.resolve(buildFileName))) {
                        return candidate;
                    }
                }
                candidate = candidate.getParent();
            }
            return null;
        }

        private static List<String> segments(Path receiver) {
            List<String> segments = new ArrayList<>();
            for (Path segment : receiver) {
                String value = segment.toString();
                if (!value.isEmpty()) {
                    segments.add(value);
                }
            }
            return segments;
        }

        // consulo: Kotlin Path.relativeToOrNull
        private static @Nullable Path relativeToOrNull(Path receiver, Path base) {
            try {
                return base.normalize().relativize(receiver.normalize());
            }
            catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
