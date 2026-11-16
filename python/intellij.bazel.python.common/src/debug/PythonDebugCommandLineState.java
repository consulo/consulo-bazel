package org.jetbrains.bazel.python.debug;

import com.jetbrains.python.impl.debugger.PyDebugRunner;
import com.jetbrains.python.impl.run.PythonCommandLineState;
import com.jetbrains.python.impl.run.PythonConfigurationType;
import com.jetbrains.python.impl.run.PythonRunConfiguration;
import com.jetbrains.python.impl.run.PythonScriptCommandLineState;
import com.jetbrains.python.impl.sdk.PythonSdkType;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.content.bundle.Sdk;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.task.BazelRunTaskListener;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.CompileParams;
import org.jspecify.annotations.Nullable;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PythonDebugCommandLineState extends BazelCommandLineStateBase {
    private final ExecutionEnvironment myEnvironment;
    private final @Nullable String myRunConfigArguments;
    private final @Nullable String myAdditionalBazelParams;
    private final @Nullable Label myTarget;

    public PythonDebugCommandLineState(
        ExecutionEnvironment environment,
        @Nullable String runConfigArguments,
        @Nullable String additionalBazelParams
    ) {
        super(environment);
        myEnvironment = environment;
        myRunConfigArguments = runConfigArguments;
        myAdditionalBazelParams = additionalBazelParams;
        myTarget = environment.getRunProfile() instanceof BazelRunConfiguration configuration && configuration.getTargets().size() == 1
            ? configuration.getTargets().get(0)
            : null;
    }

    public @Nullable String getAdditionalBazelParams() {
        return myAdditionalBazelParams;
    }

    public @Nullable Label getTarget() {
        return myTarget;
    }

    @Override
    protected BazelTaskListener createAndAddTaskListener(BazelProcessHandler handler) {
        return new BazelRunTaskListener(handler);
    }

    @Override
    protected CompletableFuture<Integer> runCommand(
        BazelServerFacade server,
        CompletableFuture<@Nullable Long> pidDeferred,
        BazelProcessHandler handler
    ) {
        BazelRunConfiguration configuration = BazelRunConfiguration.get(myEnvironment);
        // consulo: Kotlin List.single()
        if (configuration.getTargets().size() != 1) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("List must have exactly one element."));
        }
        Label targetId = configuration.getTargets().get(0);
        List<String> allFlags = new ArrayList<>(ProjectViewExtensions.getDebugFlags(server.getProjectView()));
        allFlags.addAll(ProjectViewExtensions.getPythonDebugFlags(server.getProjectView()));
        CompileParams buildParams =
            new CompileParams(
                getTaskGroupId().task("py-debug"),
                List.of(targetId),
                PythonDebugUtils.buildPythonDebugBazelArguments(allFlags, myAdditionalBazelParams)
            );
        return server.buildTargetCompile(buildParams).thenApply(ignored -> 0);
    }

    public CompletableFuture<PythonCommandLineState> asPythonState() {
        return pythonConfig().thenApply(config -> new PythonScriptCommandLineState(config, myEnvironment));
    }

    private CompletableFuture<PythonRunConfiguration> pythonConfig() {
        // consulo: the suspend function has no suspension point, an exception of it fails the returned future
        try {
            Label target = myTarget;
            PythonDebugUtils.PythonDebugInfo debugInfo = target != null
                ? PythonDebugUtils.preparePythonDebug(myEnvironment.getProject(), target)
                : null;
            if (target == null) {
                throw new IllegalStateException(BazelPluginLocalize.pythonDebugErrorNoId().get());
            }
            else if (debugInfo == null) {
                throw new IllegalStateException(BazelPluginLocalize.pythonDebugErrorOther(target).get());
            }
            // consulo: PythonConfigurationFactory is not accessible, its template configuration is created through ConfigurationFactory
            ConfigurationFactory factory = PythonConfigurationType.getInstance().getFactory();
            PythonRunConfiguration templateConfig =
                (PythonRunConfiguration) factory
                    .createTemplateConfiguration(myEnvironment.getProject()); // should always succeed; that's what PythonConfigurationFactory produces
            templateConfig.setInterpreterOptions("-X frozen_modules=off"); // frozen modules improve performance but obscure stack traces
            templateConfig.setScriptName(debugInfo.pythonFile().toAbsolutePath().toString());
            templateConfig.setScriptParameters(PythonDebugUtils.buildPythonDebugScriptParameters(debugInfo.targetArgs(), myRunConfigArguments));
            templateConfig.setWorkingDirectory(debugInfo.workingDirectory().toAbsolutePath().toString());
            // The sync sets an interpreter on the module of each target, so the debug run only looks one up.
            Path pythonBinary = debugInfo.pythonBinary();
            Sdk sdkForPythonBinary = pythonBinary != null ? getSdkForPythonBinary(pythonBinary) : null;
            Sdk sdk = sdkForPythonBinary != null ? sdkForPythonBinary : getSdkForTarget(myEnvironment.getProject(), target);
            Map<String, String> envs = new LinkedHashMap<>(debugInfo.environmentVariables());
            envs.put(PyDebugRunner.LIBRARY_ROOTS, PythonDebugUtils.buildPythonLibraryRoots(sdk, debugInfo.libraryRoots()));
            templateConfig.setEnvs(envs);
            // consulo: a Consulo Python run configuration has no `sdk` property, it keeps the SDK home when the module SDK is not used
            templateConfig.setUseModuleSdk(false);
            templateConfig.setSdkHome(sdk.getHomePath());
            return CompletableFuture.completedFuture(templateConfig);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // consulo: upstream top-level functions of PythonDebugCommandLineState.kt

    public static @Nullable Sdk getSdkForPythonBinary(Path pythonBinary) {
        if (!pythonBinary.isAbsolute()) {
            return null;
        }
        Path normalizedPythonBinary = toSdkLookupPath(pythonBinary);
        // consulo: PythonSdkUtil.getAllSdksOn(pythonBinary.getEelDescriptor()) -> PythonSdkType.getAllSdks() (EEL is not ported)
        for (Sdk sdk : PythonSdkType.getAllSdks()) {
            String sdkHome = sdk.getHomePath();
            if (sdkHome == null) {
                continue;
            }
            boolean matches;
            try {
                matches = toSdkLookupPath(Path.of(sdkHome)).equals(normalizedPythonBinary);
            }
            catch (Throwable e) {
                matches = false;
            }
            if (matches) {
                return sdk;
            }
        }
        return null;
    }

    private static Path toSdkLookupPath(Path receiver) {
        try {
            return receiver.toRealPath(LinkOption.NOFOLLOW_LINKS);
        }
        catch (Throwable e) {
            return receiver.toAbsolutePath().normalize();
        }
    }

    private static Sdk getSdkForTarget(Project project, Label target) {
        // consulo: the module of the target from ModuleTargetService (no workspace model storage); upstream takes the name of the
        // first SdkDependency of the module, in Consulo the Python SDK is the SDK of the Python module extension
        List<Module> modules = project.getInstance(ModuleTargetService.class).findModulesByLabel(target);
        Module module = modules.isEmpty() ? null : modules.get(0);
        Sdk moduleSdk = module != null ? PythonSdkType.findPythonSdk(module) : null; // module's SDK dependency
        String sdkName = moduleSdk != null ? moduleSdk.getName() : null;
        if (sdkName != null) {
            // the first SDK matching the module SDK dependency
            for (Sdk sdk : PythonSdkType.getAllSdks()) {
                if (sdk.getName().equals(sdkName)) {
                    return sdk;
                }
            }
        }
        throw new IllegalStateException(BazelPluginLocalize.pythonDebugErrorNoSdk(target).get());
    }
}
