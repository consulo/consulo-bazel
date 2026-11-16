/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.dotnet.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.bazel.dotnet.localize.BazelDotNetLocalize;
import consulo.bazel.dotnet.module.extension.BazelDotNetCoreModuleExtension;
import consulo.content.bundle.Sdk;
import consulo.dotnet.debugger.impl.DotNetDebugProcess;
import consulo.dotnet.run.DotNetRunProfileState;
import consulo.dotnet.util.DebugConnectionInfo;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.runner.AsyncProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.GenericRunConfigurationState;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.utils.ExecutableInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

/**
 * Debugs a C# binary or test: builds it with {@code bazel run --script_path} (with the {@code debug_flags} of the project view),
 * reads the assembly, its working directory and environment from the run script and debugs the assembly with netcoredbg of
 * incubating-consulo-dotnet-core ({@link BazelDotNetCoreModuleExtension#createCommandLine},
 * {@link BazelDotNetCoreModuleExtension#createDebuggerProcess}), like consulo-dotnet's {@code DotNetDebuggerProgramRunner}.
 * <p>
 * The assembly is run with {@code dotnet <assembly>} in the runfiles directory of the launcher: the {@code deps.json} of
 * rules_dotnet names the assemblies of the dependencies by their runfiles path, which the probing paths of the
 * {@code runtimeconfig.json} resolve against the working directory.
 * <p>
 * consulo: Consulo-only, in the spirit of {@code BazelGoDebugRunner} / {@code CcDebugRunner}.
 * TODO consulo: a test is debugged like a binary (no test console, no test filter: the NUnit shim of rules_dotnet reads no
 * {@code TESTBRIDGE_TEST_ONLY}).
 */
@ExtensionImpl
public class BazelDotNetDebugRunner extends AsyncProgramRunner<RunnerSettings> {
    private static final Logger LOG = Logger.getInstance(BazelDotNetDebugRunner.class);

    private static final String HOST = "127.0.0.1";

    @Override
    public String getRunnerId() {
        return "BazelDotNetDebugRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (!executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            return false;
        }
        if (!(profile instanceof BazelRunConfiguration configuration) || configuration.getTargets().size() != 1) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return (handler instanceof BazelDotNetRunHandler || handler instanceof BazelDotNetTestHandler) &&
            // the module of the target is replaced by a sync
            !SyncStatusService.isSyncInProgress(configuration.getProject());
    }

    @Override
    protected CompletableFuture<RunContentDescriptor> executeImpl(RunProfileState state, ExecutionEnvironment environment)
        throws ExecutionException {
        if (!(state instanceof BazelDotNetDebugState debugState)) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoSingleTarget());
        }
        List<Label> targets = BazelRunConfiguration.get(environment).getTargets();
        if (targets.size() != 1) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoSingleTarget());
        }
        Label label = targets.get(0);
        Project project = environment.getProject();

        BazelDotNetCoreModuleExtension extension = findExtension(project, label);
        Sdk sdk = extension.getSdk();
        if (sdk == null) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoSdk(extension.getModule().getName()));
        }

        GenericRunConfigurationState bazelState = debugState.getBazelState();
        Path scriptPath = ScriptPathBuildTargetTask.createTempScriptFile();
        ScriptPathBuildTargetTask buildTask = new ScriptPathBuildTargetTask(
            scriptPath,
            List.of(),
            Utils.parseAsProgramArguments(bazelState.getAdditionalBazelParams())
        );
        return BuildTargetTask.runBuildTargetTask(List.of(label), project, true, buildTask)
            .thenApply(status -> {
                if (status != BazelStatus.SUCCESS) {
                    throw new CompletionException(new ExecutionException(BazelDotNetLocalize.debugErrorBuildFailed(label, status)));
                }
                return readExecutableInfo(scriptPath, label);
            })
            .thenCompose(executableInfo -> project.getUIAccess().giveAsync(() -> {
                try {
                    return startSession(environment, extension, sdk, bazelState, executableInfo);
                }
                catch (ExecutionException e) {
                    throw new CompletionException(e);
                }
            }))
            .whenComplete((descriptor, error) -> {
                try {
                    Files.deleteIfExists(scriptPath);
                }
                catch (IOException | RuntimeException ignored) {
                }
            });
    }

    private static BazelDotNetCoreModuleExtension findExtension(Project project, Label label) throws ExecutionException {
        BazelDotNetCoreModuleExtension extension = ReadAction.compute(() -> {
            for (Module module : project.getInstance(ModuleTargetService.class).findModulesByLabel(label)) {
                BazelDotNetCoreModuleExtension it = ModuleUtilCore.getExtension(module, BazelDotNetCoreModuleExtension.class);
                if (it != null) {
                    return it;
                }
            }
            return null;
        });
        if (extension == null) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoModule(label));
        }
        return extension;
    }

    private static ExecutableInfo readExecutableInfo(Path scriptPath, Label label) {
        try {
            ExecutableInfo executableInfo = ExecutableInfo.fromBazelRunScript(Files.readString(scriptPath));
            executableInfo.createRequiredDirectories();
            linkDebugSymbols(executableInfo.workingDir());
            return executableInfo;
        }
        catch (Exception e) {
            throw new CompletionException(new ExecutionException(BazelDotNetLocalize.debugErrorScriptFailed(label, String.valueOf(e.getMessage())).get(), e));
        }
    }

    private RunContentDescriptor startSession(
        ExecutionEnvironment environment,
        BazelDotNetCoreModuleExtension extension,
        Sdk sdk,
        GenericRunConfigurationState bazelState,
        ExecutableInfo executableInfo
    ) throws ExecutionException {
        Path assembly = assemblyOf(executableInfo.binary());

        DebugConnectionInfo debugConnectionInfo = new DebugConnectionInfo(HOST, -1, true);
        GeneralCommandLine commandLine = extension.createCommandLine(sdk, assembly.toString(), debugConnectionInfo);
        commandLine.addParameters(executableInfo.args());
        commandLine.addParameters(Utils.parseAsProgramArguments(bazelState.getProgramArguments()));
        Map<String, String> env = new LinkedHashMap<>(executableInfo.envVars());
        // custom environment variables are not preserved by the script file
        env.putAll(bazelState.getEnv().getEnvs());
        commandLine.withEnvironment(env);
        commandLine.withParentEnvironmentType(
            bazelState.getEnv().isPassParentEnvs() ? GeneralCommandLine.ParentEnvironmentType.CONSOLE : GeneralCommandLine.ParentEnvironmentType.NONE
        );
        commandLine.withWorkingDirectory(executableInfo.workingDir());

        DotNetRunProfileState runState = new DotNetRunProfileState(environment, commandLine);
        XDebugSession debugSession = XDebuggerManager.getInstance(environment.getProject()).startSession(environment, session -> {
            DotNetDebugProcess process = extension.createDebuggerProcess(
                session,
                new BazelDotNetDebugRunProfile(environment.getRunProfile(), extension.getModule()),
                debugConnectionInfo
            );
            process.setExecutionResult(runState.execute(environment.getExecutor(), this));
            // netcoredbg is the server, it listens before the program starts
            process.start();
            return (XDebugProcess) process;
        });
        return debugSession.getRunContentDescriptor();
    }

    /**
     * rules_dotnet compiles with {@code /debug:portable}, but the runfiles of a binary only link the assemblies of its dependencies,
     * not their {@code .pdb} files, and netcoredbg only looks for the symbols of an assembly next to the loaded file. So the
     * {@code .pdb} next to the linked file in the output tree is linked next to the runfiles link, otherwise breakpoints in the
     * dependencies are never hit.
     * <p>
     * TODO consulo: rules_dotnet could put the {@code .pdb} files of {@code DotnetAssemblyRuntimeInfo.pdbs} into the runfiles
     */
    private static void linkDebugSymbols(Path runfilesDir) {
        if (!Files.isDirectory(runfilesDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(runfilesDir)) {
            files.filter(it -> it.getFileName().toString().endsWith(".dll") && Files.isSymbolicLink(it)).forEach(link -> {
                Path pdbLink = withPdbExtension(link);
                if (Files.exists(pdbLink, LinkOption.NOFOLLOW_LINKS)) {
                    return;
                }
                try {
                    Path pdb = withPdbExtension(link.toRealPath());
                    if (Files.isRegularFile(pdb)) {
                        Files.createSymbolicLink(pdbLink, pdb);
                    }
                }
                catch (IOException | UnsupportedOperationException e) {
                    LOG.debug("Cannot link the symbols of " + link, e);
                }
            });
        }
        catch (IOException | UncheckedIOException e) {
            LOG.debug("Cannot link the symbols of " + runfilesDir, e);
        }
    }

    private static Path withPdbExtension(Path assembly) {
        String name = assembly.getFileName().toString();
        return assembly.resolveSibling(name.substring(0, name.length() - ".dll".length()) + ".pdb");
    }

    /**
     * The assembly of a rules_dotnet launcher ({@code app.dll.sh}, {@code app.dll.bat}, ...: {@code dotnet exec app.dll}), in the
     * output directory, where its {@code .pdb} is (the runfiles only link the assembly).
     */
    private static Path assemblyOf(Path launcher) throws ExecutionException {
        Path fileName = launcher.getFileName();
        Path directory = launcher.getParent();
        String name = fileName == null ? "" : fileName.toString();
        int index = name.lastIndexOf(".dll");
        if (directory == null || index < 0) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoAssembly(launcher));
        }
        Path assembly = directory.resolve(name.substring(0, index + ".dll".length()));
        if (!Files.isRegularFile(assembly)) {
            throw new ExecutionException(BazelDotNetLocalize.debugErrorNoAssembly(launcher));
        }
        try {
            return assembly.toRealPath();
        }
        catch (IOException e) {
            return assembly;
        }
    }
}
