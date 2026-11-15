package org.jetbrains.bazel.server.tasks;

import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.TaskId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public class ScriptPathBuildTargetTask implements BuildTargetTask {
    private final Path myScriptPath;
    private final List<String> myProgramArguments;
    private final List<String> myAdditionalBazelParams;

    public ScriptPathBuildTargetTask(
        Path scriptPath,
        List<String> programArguments,
        List<String> additionalBazelParams
    ) {
        myScriptPath = scriptPath;
        myProgramArguments = programArguments;
        myAdditionalBazelParams = additionalBazelParams;
    }

    @Override
    public CompletableFuture<BazelStatus> build(
        BazelServerFacade server,
        List<Label> targetIds,
        TaskConsole buildConsole,
        TaskId taskId,
        List<String> debugFlags
    ) {
        // consulo: Kotlin List.single()
        if (targetIds.isEmpty()) {
            throw new NoSuchElementException("List is empty.");
        }
        if (targetIds.size() > 1) {
            throw new IllegalArgumentException("List has more than one element.");
        }
        List<String> additionalBazelParams = new ArrayList<>(scriptPathParams(myScriptPath));
        additionalBazelParams.addAll(debugFlags);
        additionalBazelParams.addAll(myAdditionalBazelParams);
        RunParams params = new RunParams(
            /* taskId = */ taskId,
            /* target = */ targetIds.get(0),
            /* checkVisibility = */ true,
            /* arguments = */ myProgramArguments,
            /* environmentVariables = */ Map.of(),
            /* additionalBazelParams = */ additionalBazelParams,
            /* pidDeferred = */ null,
            /* captureStdout = */ false
        );
        return server.buildTargetRun(params).thenApply(it -> it.statusCode());
    }

    /**
     * Returns the Bazel flags that make `bazel run` write a script for the target to {@code scriptPath} instead of running it.
     */
    public static List<String> scriptPathParams(Path scriptPath) {
        return List.of("--script_path=" + scriptPath, "--test_sharding_strategy=disabled");
    }

    public static Path createTempScriptFile() {
        // on Windows, the only way to have an executable script is to have a
        // .bat file, but on unix the extension doesn't matter
        String suffix = Platform.current().os().isWindows() ? ".bat" : "";
        Path it;
        try {
            it = Files.createTempFile(Paths.get(FileUtil.getTempDirectory()), "bazel-script-", suffix);
        }
        catch (IOException e) {
            // consulo: Kotlin has no checked exceptions
            throw new UncheckedIOException(e);
        }
        it.toFile().deleteOnExit();
        return it;
    }
}
