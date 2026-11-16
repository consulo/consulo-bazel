package org.jetbrains.bazel.clion.run.debug;

import consulo.execution.configuration.CommandLineState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.nativeDev.debugger.NativeDebuggerProvider;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.clion.run.debug.collectTargetInfo.CcDebugTargetInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.run.commandLine.Utils;
import org.jetbrains.bazel.run.state.AbstractGenericTestState;
import org.jetbrains.bazel.run.state.GenericRunConfigurationState;
import org.jetbrains.bazel.run.state.Test.HasTestFilter;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.utils.ExecutableInfo;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// consulo: CidrCommandLineState (com.jetbrains.cidr.execution) does not exist; the state is a plain CommandLineState that keeps what
// CcDebugRunner collects, CcDebugRunner starts the session with consulo-native-dev's NativeDebugSessionStarter
public class CcDebugCommandLineState extends CommandLineState {
    private final GenericRunConfigurationState myBazelState;

    // consulo: upstream passes the launcher to the CidrCommandLineState constructor
    private final CcCidrLauncher myLauncher;

    // set by the CcDebugRunner
    private @Nullable CcDebugTargetInfo myTargetInfo;
    private @Nullable ExecutableInfo myExecutionInfo;
    // consulo: upstream `var debugProfile: CidrDebugProfile<*>?` (the CLion debugger toolchain chosen in the settings) is dropped,
    //   consulo-native-dev chooses the debugger (gdb, lldb, ... over DAP) when the session starts

    public CcDebugCommandLineState(
        ExecutionEnvironment environment,
        GenericRunConfigurationState bazelState
    ) {
        super(environment);
        myBazelState = bazelState;
        myLauncher = new CcCidrLauncher(environment, bazelState);
    }

    public GenericRunConfigurationState getBazelState() {
        return myBazelState;
    }

    CcCidrLauncher getLauncher() {
        return myLauncher;
    }

    public @Nullable CcDebugTargetInfo getTargetInfo() {
        return myTargetInfo;
    }

    public void setTargetInfo(@Nullable CcDebugTargetInfo targetInfo) {
        myTargetInfo = targetInfo;
    }

    public @Nullable ExecutableInfo getExecutionInfo() {
        return myExecutionInfo;
    }

    public void setExecutionInfo(@Nullable ExecutableInfo executionInfo) {
        myExecutionInfo = executionInfo;
    }

    // consulo: upstream CcCidrLauncher.createProcess(state)
    @Override
    protected ProcessHandler startProcess() {
        throw new UnsupportedOperationException("run is handled by the generic Bazel handler");
    }
}

// consulo: CidrLauncher (com.jetbrains.cidr.execution) does not exist. createDebugProcess() becomes createDebugTarget(): instead of a
// CidrLocalDebugProcess with a GDB/LLDB/DAP DebuggerDriver it describes the launch for consulo-native-dev, whose debugger runs the
// source mapping commands of CcDebuggerSetup
class CcCidrLauncher {
    private static final Path PROC_CWD = Path.of("/proc", "self", "cwd");

    // TODO consulo: CPPEnvironment / CPPToolchains (com.jetbrains.cidr.cpp.toolchains) do not exist, the debugger environment is
    //   chosen by consulo-native-dev
    // // the environment does not affect the debugger
    // private val DEBUG_ENVIRONMENT = CPPEnvironment(CPPToolchains.Toolchain(OS.CURRENT))

    private final ExecutionEnvironment myEnvironment;
    private final GenericRunConfigurationState myBazelState;

    CcCidrLauncher(ExecutionEnvironment environment, GenericRunConfigurationState bazelState) {
        myEnvironment = environment;
        myBazelState = bazelState;
    }

    public Project getProject() {
        return myEnvironment.getProject();
    }

    // consulo: upstream createDebugProcess(state: CommandLineState, session: XDebugSession), the state is cast to CcDebugCommandLineState
    public NativeDebugTarget createDebugTarget(CcDebugCommandLineState ccState) {
        Project project = getProject();

        // the CcDebugRunner sets these before the debug process starts
        CcDebugTargetInfo targetInfo = Objects.requireNonNull(ccState.getTargetInfo());
        ExecutableInfo executionInfo = Objects.requireNonNull(ccState.getExecutionInfo());

        // TODO: when can these be null and what to do then?
        WorkspaceSnapshot snapshot = Objects.requireNonNull(WorkspaceHelpers.getWorkspaceSnapshotOrNull(project));
        Path executionRoot = Objects.requireNonNull(snapshot.bazelInfo().execRoot());
        Path workspaceRoot = Objects.requireNonNull(
            toNioPathOrNull(BazelProjectInfoService.getBazelProjectInfo(project).getProjectRootDir())
        );

        // TODO consulo: CidrDebugProfile / DapDebugProfileState (debuggerWorkingDir) do not exist; consulo-native-dev starts the
        //   debugger in its own working directory, there is no API for a debugger working directory
        // if (OS.CURRENT == OS.macOS) {
        //   // on macos the debugger resolves debug symbols relative to the working directory
        //   debugProfile = debugProfile.withDapWorkingDir(executionRoot)
        // }

        // TODO consulo: CidrDebugProfile.createDriverConfiguration(project, isElevated, isEmulateTerminal, environment) does not exist,
        //   consulo-native-dev creates the debugger from the NativeDebugTarget
        // val debugDriver = debugProfile.createDriverConfiguration(
        //   project = project,
        //   isElevated = false,
        //   isEmulateTerminal = false,
        //   environment = DEBUG_ENVIRONMENT,
        // )

        // external paths must be mapped before the workspace root, since one is a prefix of the other
        Map<Path, Path> sourceMappings = new LinkedHashMap<>();
        // /proc/self/cwd mappings used on linux
        putLocalRepoMapping(sourceMappings, PROC_CWD, workspaceRoot, targetInfo.repoMapping());
        sourceMappings.put(PROC_CWD.resolve("external"), executionRoot.resolve("external"));
        sourceMappings.put(PROC_CWD, workspaceRoot);

        // execution root mappings used on Windows
        sourceMappings.put(executionRoot.resolve("external"), executionRoot.resolve("external")); // required to specialize the one below
        sourceMappings.put(executionRoot, workspaceRoot);

        // relative mappings used on macOS
        putLocalRepoMapping(sourceMappings, Path.of(""), workspaceRoot, targetInfo.repoMapping());
        sourceMappings.put(Path.of("external"), executionRoot.resolve("external"));
        sourceMappings.put(Path.of(""), workspaceRoot);

        boolean isTest = myBazelState instanceof AbstractGenericTestState<?>;
        Map<String, String> env = new LinkedHashMap<>(executionInfo.envVars());
        // custom environment variables are not preserved by the script file
        env.putAll(myBazelState.getEnv().getEnvs());
        GeneralCommandLine commandLine = Utils.scriptPathCommandLine(
            /* scriptPath = */ executionInfo.binary(),
            /* env = */ env,
            /* isTest = */ isTest,
            /* isPassParentEnvs = */ myBazelState.getEnv().isPassParentEnvs(),
            /* testFilter = */ myBazelState instanceof HasTestFilter hasTestFilter ? hasTestFilter.getTestFilter() : null,
            /* additionalScriptParameters = */ executionInfo.args()
        ).withWorkingDirectory(executionInfo.workingDir());

        // TODO consulo: createDebugProcessForTest is not ported: NativeDebugSessionStarter creates the debug process and its console,
        //   there is no API to give it the SMTRunner test console of the run configuration (CcTestConsoleBuilder:
        //   BazelRunConfiguration.createTestConsoleProperties), the rerun failed tests restart action
        //   (XDebugSessionImpl.addRestartActions) and the test XML report on process termination
        //   (TestXmlParser(BspClientTestNotifier(taskHandler)).parseAndReport(taskGroupId.task("test"), executionInfo.testXml,
        //   CC_TEST_KIND));
        //   a test target is debugged like a binary

        // consulo: upstream TrivialRunParameters(debugDriver, TrivialInstaller(commandLine)) + CidrLocalDebugProcess on the EDT;
        //   the command line becomes a launch target, the session is started by CcDebugRunner
        File workDirectory = commandLine.getWorkDirectory();
        NativeDebugTarget target = NativeDebugTarget.launch(
            Path.of(commandLine.getExePath()),
            commandLine.getParametersList().getList(),
            workDirectory == null ? null : workDirectory.toPath(),
            commandLine.getEffectiveEnvironment(),
            null
        );

        // consulo: upstream process.postCommand { driver -> configureLldbDriver / configureGdbDriver / configureDapDriver } by the debug
        //   profile type; the debugger runs the init commands of the setup, chosen by its command family
        return target.withSetup(new CcDebuggerSetup(sourceMappings));
    }

    // consulo: upstream VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private static void putLocalRepoMapping(Map<Path, Path> receiver, Path execroot, Path workspace, RepoMapping repoMapping) {
        for (Map.Entry<String, Path> entry : RepoMapping.getLocalRepositories(repoMapping).localRepositories().entrySet()) {
            receiver.put(execroot.resolve("external").resolve(entry.getKey()), workspace.resolve(entry.getValue()));
        }
    }

    // consulo: upstream configureGdbDriver(driver, sourceMappings) runs driver.executeInterpreterCommand, here it returns the commands
    static List<String> configureGdbDriver(Map<Path, Path> sourceMappings) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Path, Path> entry : sourceMappings.entrySet()) {
            commands.add("set substitute-path \"" + entry.getKey() + "\" \"" + entry.getValue() + "\"");
        }
        return commands;
    }

    // consulo: upstream configureLldbDriver(driver, sourceMappings) runs driver.executeInterpreterCommand, here it returns the command
    static List<String> configureLldbDriver(Map<Path, Path> sourceMappings) {
        StringBuilder command = new StringBuilder();
        command.append("settings set --global -- target.source-map");
        for (Map.Entry<Path, Path> entry : sourceMappings.entrySet()) {
            command.append(" \"").append(entry.getKey()).append("\" \"").append(entry.getValue()).append('"');
        }

        return List.of(command.toString());
    }

    // TODO consulo: CidrDebugProfile.withDapWorkingDir (DapDebugProfileState.debuggerWorkingDir) is not ported, see createDebugTarget
    //  /**
    //   * Gives a DAP debug profile without a debugger working directory the [executionRoot] as that directory.
    //   * On macOS, the binary records its object files relative to the execution root. LLDB finds them relative
    //   * to its working directory.
    //   */
    //  private fun CidrDebugProfile<*>.withDapWorkingDir(executionRoot: Path): CidrDebugProfile<*>

    // TODO consulo: consulo-native-dev has no DAP path mapping API (DebuggerDriver.addPathMapping); its DAP debuggers belong to the gdb or
    //   lldb command family and get the init commands of CcDebuggerSetup instead
    //  private fun configureDapDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
    //    // the DAP driver gives an IDE path the debugger path of the first mapping with the longest IDE path,
    //    // and only a relative debugger path matches both `lib/impl.cc` and the `./lib/hdr.h` that the compiler records for a header
    //    val relativeFirst = sourceMappings.entries.sortedBy { it.key.isAbsolute }
    //    for ([index, mapping] in relativeFirst.withIndex()) {
    //      driver.addPathMapping(index, mapping.key.toString(), mapping.value.toString())
    //    }
    //  }
}

// consulo: upstream `when (debugProfile.type.getId()) { CLionLldbDebugProfileType.ID -> ...; CLionGdbDebugProfileType.ID -> ...;
//   DapDebugProfileType.ID -> ... }` in process.postCommand { driver -> }; consulo-native-dev debuggers report their command family
//   instead of a profile type
class CcDebuggerSetup implements NativeDebuggerSetup {
    private final Map<Path, Path> mySourceMappings;

    CcDebuggerSetup(Map<Path, Path> sourceMappings) {
        mySourceMappings = sourceMappings;
    }

    @Override
    public List<String> getInitCommands(NativeDebuggerProvider debugger) {
        return switch (debugger.getFamilyId()) {
            case NativeDebuggerProvider.LLDB_FAMILY -> CcCidrLauncher.configureLldbDriver(mySourceMappings);
            case NativeDebuggerProvider.GDB_FAMILY -> CcCidrLauncher.configureGdbDriver(mySourceMappings);
            default -> List.of();
        };
    }
}
