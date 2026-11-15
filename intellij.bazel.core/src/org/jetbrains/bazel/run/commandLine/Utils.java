package org.jetbrains.bazel.run.commandLine;

import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersListUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class Utils {
    private Utils() {
    }

    public static List<String> parseAsProgramArguments(@Nullable String value) {
        if (value == null) {
            return List.of();
        }
        else {
            return ParametersListUtil.parse(
                /* parameterString = */ value,
                /* keepQuotes = */ false,
                /* supportSingleQuotes = */ true
            );
        }
    }

    // See: https://bazel.build/reference/test-encyclopedia
    public static final String BAZEL_TEST_FILTER_ENV = "TESTBRIDGE_TEST_ONLY";

    public static GeneralCommandLine scriptPathCommandLine(
        Path scriptPath,
        Map<String, String> env,
        boolean isTest,
        boolean isPassParentEnvs,
        @Nullable String testFilter,
        List<String> additionalScriptParameters
    ) {
        GeneralCommandLine.ParentEnvironmentType parentEnvironment;
        if (isTest || !isPassParentEnvs) {
            // Bazel tests don't receive the full environment because of sandboxing
            parentEnvironment = GeneralCommandLine.ParentEnvironmentType.NONE;
        }
        else {
            parentEnvironment = GeneralCommandLine.ParentEnvironmentType.CONSOLE;
        }
        GeneralCommandLine commandLine =
            new GeneralCommandLine()
                .withExePath(scriptPath.toString())
                .withParentEnvironmentType(parentEnvironment)
                .withEnvironment(env)
                .withParameters(additionalScriptParameters);
        if (testFilter != null) {
            commandLine.getEnvironment().put(BAZEL_TEST_FILTER_ENV, testFilter);
        }
        return commandLine;
    }
}
