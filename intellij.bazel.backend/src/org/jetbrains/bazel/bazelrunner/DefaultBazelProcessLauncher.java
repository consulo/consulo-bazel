package org.jetbrains.bazel.bazelrunner;

import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.GeneralCommandLine.ParentEnvironmentType;
import org.jetbrains.bazel.bazelrunner.BazelCommand.BazelCommandExecutionDescriptor;
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher.BazelProcessLauncherProvider;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class DefaultBazelProcessLauncher implements BazelProcessLauncher {
    private final Path myWorkspaceRoot;
    private final Map<String, String> myParentEnvironment;

    DefaultBazelProcessLauncher(Path workspaceRoot, Map<String, String> parentEnvironment) {
        myWorkspaceRoot = workspaceRoot;
        myParentEnvironment = parentEnvironment;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Process launchProcess(BazelCommandExecutionDescriptor executionDescriptor) {
        GeneralCommandLine commandLine;
        // TODO consulo: PtyCommandLine is not part of the Consulo process API (only process-impl internal), the pty branch is not ported
        // if (executionDescriptor.enablePty() && Application.get().getInstance(BazelApplicationContextService.class).getEnablePty()
        //     && BazelFeatureFlags.getUsePty()) {
        //     commandLine = new PtyCommandLine(executionDescriptor.command())
        //         .withConsoleMode(true)
        //         .withInitialColumns(512)
        //         .withInitialRows(25);
        // }
        // else {
        commandLine = new GeneralCommandLine(executionDescriptor.command());
        // }
        commandLine.withParentEnvironmentType(ParentEnvironmentType.NONE);
        // PtyCommandLine sets the environment variable `TERM` if not set already in the environment provided. To avoid having different
        // values for the environment variables in our bazel invocations, we `TERM` to a sensible value so that PtyCommandLine will not
        // change the environment we set.
        // While normally nothing should depend on the value `TERM`, we have to work around bazel issue
        // https://github.com/bazelbuild/bazel/issues/29956 which is not fixed in all versions of bazel in use.
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("TERM", "xterm-256color");
        environment.putAll(myParentEnvironment);
        environment.putAll(BazelProxyForwarding.environment());
        environment.putAll(executionDescriptor.environment());
        commandLine.withEnvironment(environment);
        commandLine.withWorkingDirectory(myWorkspaceRoot);
        commandLine.withRedirectErrorStream(false);

        // consulo: the process handler builder returns a ProcessHandler, the raw Process is needed here
        try {
            return commandLine.createProcess();
        }
        catch (ExecutionException e) {
            // consulo: Kotlin has no checked exceptions, upstream propagates ExecutionException as is
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}

final class DefaultBazelProcessLauncherProvider implements BazelProcessLauncherProvider {
    static final DefaultBazelProcessLauncherProvider INSTANCE = new DefaultBazelProcessLauncherProvider();

    private DefaultBazelProcessLauncherProvider() {
    }

    @Override
    public BazelProcessLauncher createBazelProcessLauncher(
        Path workspaceRoot,
        Map<String, String> parentEnvironment
    ) {
        return new DefaultBazelProcessLauncher(workspaceRoot, parentEnvironment);
    }
}
