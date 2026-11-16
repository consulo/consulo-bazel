package org.jetbrains.bazel.commons;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import org.jetbrains.bazel.commons.RepoMapping.BzlmodRepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.Canonical;
import org.jetbrains.bazel.label.Label.Main;
import org.jetbrains.bazel.label.Label.RepoType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BazelPathsResolver {
    private final BazelInfo myBazelInfo;

    public BazelPathsResolver(BazelInfo bazelInfo) {
        myBazelInfo = bazelInfo;
    }

    public Path workspaceRoot() {
        return myBazelInfo.workspaceRoot();
    }

    public Path bazelBin() {
        return myBazelInfo.bazelBin();
    }

    public List<Path> resolvePaths(List<ArtifactLocation> fileLocations, LocalRepositoryMapping localRepositories) {
        List<Path> result = new ArrayList<>(fileLocations.size());
        for (ArtifactLocation it : fileLocations) {
            result.add(resolve(it, localRepositories));
        }
        return result;
    }

    public Path resolve(BuildEventStreamProtos.File file) {
        return Path.of(URI.create(file.getUri()));
    }

    public boolean isExternal(
        ArtifactLocation fileLocation,
        LocalRepositoryMapping localRepositories
    ) {
        if (fileLocation.getRootPath().startsWith("../") || fileLocation.getRootPath().startsWith("external/")) {
            String[] rootSegments = fileLocation.getRootPath().split("/", -1);
            return !localRepositories.localRepositories().containsKey(rootSegments[1]);
        }
        else if (fileLocation.getRelativePath().startsWith("external/")) {
            String[] relativeSegments = fileLocation.getRelativePath().split("/", -1);
            return !localRepositories.localRepositories().containsKey(relativeSegments[1]);
        }
        else {
            return false;
        }
    }

    private ArtifactLocation mapLocalRepositories(ArtifactLocation fileLocation, LocalRepositoryMapping localRepositories) {
        if (!(fileLocation.getRootPath().startsWith("../") || fileLocation.getRootPath().startsWith("external/"))) {
            return fileLocation;
        }
        String[] rootSegments = fileLocation.getRootPath().split("/", -1);
        Path localPath = localRepositories.localRepositories().get(rootSegments[1]);
        if (localPath == null) {
            return fileLocation;
        }
        // Update relative path, keeping bazel's file-system hierarchy separator
        String newRelativePath = invariantSeparatorsPathString(localPath) + "/" + fileLocation.getRelativePath();
        return ArtifactLocation.newBuilder()
            .setRootPath("")
            .setRelativePath(newRelativePath)
            .setIsSource(fileLocation.getIsSource())
            .build();
    }

    public Path resolve(ArtifactLocation fileLocation, LocalRepositoryMapping localRepositories) {
        ArtifactLocation mappedFileLocation = mapLocalRepositories(fileLocation, localRepositories);
        if (isAbsolute(mappedFileLocation)) {
            return resolveAbsolute(mappedFileLocation);
        }
        else if (isMainWorkspaceSource(mappedFileLocation, localRepositories)) {
            return resolveSource(mappedFileLocation);
        }
        else if (isInExternalWorkspace(mappedFileLocation)) {
            return resolveExternal(mappedFileLocation);
        }
        else {
            return resolveOutput(mappedFileLocation);
        }
    }

    private boolean isAbsolute(ArtifactLocation fileLocation) {
        String relative = fileLocation.getRelativePath();
        return relative.startsWith("/") && Files.exists(Paths.get(relative));
    }

    private Path resolveAbsolute(ArtifactLocation fileLocation) {
        return Paths.get(fileLocation.getRelativePath());
    }

    private Path resolveExternal(ArtifactLocation fileLocation) {
        Path outputBaseRelativePath = Paths.get(fileLocation.getRootPath(), fileLocation.getRelativePath());
        return resolveExternal(outputBaseRelativePath);
    }

    private Path resolveExternal(Path outputBaseRelativePath) {
        return myBazelInfo
            .outputBase()
            .resolve(outputBaseRelativePath);
    }

    public Path resolveOutput(ArtifactLocation fileLocation) {
        Path execRootRelativePath = Paths.get(fileLocation.getRootPath(), fileLocation.getRelativePath());
        return resolveOutput(execRootRelativePath);
    }

    public Path resolveOutput(Path execRootRelativePath) {
        if (execRootRelativePath.startsWith("external")) {
            return resolveExternal(execRootRelativePath);
        }
        else {
            return myBazelInfo.execRoot().resolve(execRootRelativePath);
        }
    }

    private Path resolveSource(ArtifactLocation fileLocation) {
        return myBazelInfo.workspaceRoot().resolve(fileLocation.getRelativePath());
    }

    private boolean isMainWorkspaceSource(ArtifactLocation fileLocation, LocalRepositoryMapping localRepositories) {
        return fileLocation.getIsSource() && !isExternal(fileLocation, localRepositories);
    }

    private boolean isInExternalWorkspace(ArtifactLocation fileLocation) {
        return fileLocation.getRootPath().startsWith("external/");
    }

    public Path relativePathToWorkspaceAbsolute(Path path) {
        return myBazelInfo.workspaceRoot().resolve(path);
    }

    public Path relativePathToExecRootAbsolute(Path path) {
        return myBazelInfo.execRoot().resolve(path);
    }

    /**
     * converts a path object to a relative path string with Bazel separator
     */
    public String getWorkspaceRelativePath(Path path) {
        return invariantSeparatorsPathString(
            myBazelInfo.workspaceRoot()
                .relativize(path)
        );
    }

    public Path resolve(Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        else if (path.startsWith("external/")) {
            return myBazelInfo.outputBase().resolve(path);
        }
        else {
            return myBazelInfo.workspaceRoot().resolve(path);
        }
    }

    public Path toDirectoryPath(ResolvedLabel label, RepoMapping repoMapping) {
        return toRepositoryPath(label.repo(), repoMapping).resolve(Label.toPath(label.packagePath()));
    }

    /**
     * Returns the root directory of the repository of {@code repo}.
     * This is the workspace root for the main repository and the source directory for a local repository.
     * For another external repository, this is its directory under the output base.
     * The path is normalized, because a {@code local_path_override} path can contain {@code .} and {@code ..} segments.
     */
    public Path toRepositoryPath(RepoType repo, RepoMapping repoMapping) {
        Path repoPath = repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping ? toRepoPath(repo, bzlmodRepoMapping) : null;
        if (repoPath == null) {
            repoPath = toRepoPathForBazel7(repo);
        }
        return repoPath.normalize();
    }

    /**
     * Returns the directory under {@code bazel-bin} that holds the outputs of the repository of {@code repo}.
     */
    public Path toBinRepositoryPath(RepoType repo, RepoMapping repoMapping) {
        if (repo instanceof Main) {
            return myBazelInfo.bazelBin();
        }
        String canonicalName = repoMapping instanceof BzlmodRepoMapping bzlmodRepoMapping ? canonicalRepoName(repo, bzlmodRepoMapping) : null;
        if (canonicalName == null) {
            canonicalName = repo.repoName();
        }
        return myBazelInfo.bazelBin().resolve("external").resolve(canonicalName);
    }

    private @Nullable Path toRepoPath(RepoType receiver, BzlmodRepoMapping repoMapping) {
        String canonicalName = canonicalRepoName(receiver, repoMapping);
        return canonicalName == null ? null : repoMapping.getCanonicalRepoNameToPath().get(canonicalName);
    }

    private @Nullable String canonicalRepoName(RepoType receiver, BzlmodRepoMapping repoMapping) {
        if (receiver instanceof Canonical || receiver instanceof Main) {
            return receiver.repoName();
        }
        else {
            return repoMapping.getApparentRepoNameToCanonicalName().get(receiver.repoName());
        }
    }

    private Path toRepoPathForBazel7(RepoType receiver) {
        if (receiver instanceof Main) {
            return myBazelInfo.workspaceRoot();
        }
        else {
            return relativePathToExecRootAbsolute(Path.of("external", receiver.repoName()));
        }
    }

    // consulo: Kotlin Path.invariantSeparatorsPathString
    private static String invariantSeparatorsPathString(Path path) {
        String separator = path.getFileSystem().getSeparator();
        return separator.equals("/") ? path.toString() : path.toString().replace(separator, "/");
    }
}
