package org.jetbrains.bazel.install;

import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.server.bsp.utils.FileUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EnvironmentCreator {
    private final Path myProjectRootDir;

    public EnvironmentCreator(Path projectRootDir) {
        myProjectRootDir = projectRootDir;
    }

    public Path create() {
        return createDotBazelBsp();
    }

    private Path createDotBazelBsp() {
        Path bazelBspDir = createDir(myProjectRootDir, Constants.DOT_BAZELBSP_DIR_NAME);
        createDotBazelBspFiles(bazelBspDir);
        return bazelBspDir;
    }

    private void createDotBazelBspFiles(Path dotBazelBspDir) {
        createGitIgnoreFile(dotBazelBspDir);
        createEmptyBuildFile(dotBazelBspDir);
    }

    private void createEmptyBuildFile(Path dotBazelBspDir) {
        FileUtils.writeIfDifferent(dotBazelBspDir.resolve(Constants.defaultBuildFileName()), "");
        try {
            Files.deleteIfExists(dotBazelBspDir.resolve(Constants.WORKSPACE_FILE_NAME));
        }
        catch (IOException e) {
            // consulo: Kotlin has no checked exceptions, upstream propagates the IOException as is
            throw new UncheckedIOException(e);
        }
    }

    public void createGitIgnoreFile(Path dotBazelBspDir) {
        FileUtils.writeIfDifferent(
            dotBazelBspDir.resolve(".gitignore"),
            """
                aspects
                .bazelproject
                .gitignore
                BUILD.bazel
                bazel_invocation_cache/output_base"""
        );
    }

    private Path createDir(Path rootDir, String name) {
        Path dir = rootDir.resolve(name);
        try {
            Files.createDirectories(dir);
        }
        catch (FileAlreadyExistsException ignored) {
        }
        catch (IOException e) {
            // consulo: Kotlin has no checked exceptions, upstream propagates the IOException as is
            throw new UncheckedIOException(e);
        }
        return dir;
    }
}
