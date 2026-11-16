package com.intellij.bazel.java.profiler;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.profiler.ProfilerConfigurationExtension;
import consulo.execution.profiler.ProfilerLaunchContext;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.process.ProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

// TODO consulo: upstream extends com.intellij.profiler.ultimate.jfr.JFRConfigurationExtensionBase (a Java RunConfigurationExtension).
//  The Consulo counterpart is the platform ProfilerConfigurationExtension, implemented for JFR by consulo-java's
//  consulo.java.profiler.impl.internal.jfr.JfrProfilerConfigurationExtension. That class, JfrProfilerConfigurationState, JfrOptions,
//  JvmProfilerProcess and consulo.java.profiler.impl.internal.jvm.JvmTargetProcess are in packages consulo.java.profiler.impl does not
//  export (it exports only consulo.java.profiler.localize), so neither the base class nor the JFR launch options and process are
//  reachable. Not registered (upstream: <runConfigurationExtension>); it would be @ExtensionImpl once they are.
//  Also needed to launch a Bazel run under "Profile with": BazelJavaProfilerStarterExtension (no starter extension point in consulo-java),
//  a program runner for BazelRunConfiguration (JavaProfilerProgramRunner requires a ModuleRunProfile with a JavaCommandLine state), and
//  JvmRunHandler.getJvmRunExtensionVmOptions/attachJvmRunExtensions calling the ProfilerConfigurationExtensions (upstream reaches this
//  class through RunConfigurationExtension.updateJavaParameters/attachToProcess; Consulo's profiler extensions are not
//  RunConfigurationExtensions)
public class BazelJFRConfigurationExtension implements ProfilerConfigurationExtension {
    // consulo: JfrProfilerConfigurationType.ID of consulo-java (not exported)
    private static final String JFR_CONFIGURATION_TYPE_ID = "java.jfr";

    public BazelJFRConfigurationExtension() {
    }

    @Override
    public boolean isApplicableFor(RunConfigurationBase configuration) {
        // TODO consulo: super.isApplicableFor(configuration) of JFRConfigurationExtensionBase
        return configuration instanceof BazelRunConfiguration;
    }

    // consulo: upstream inherits isEnabledFor() from JFRConfigurationExtensionBase: the extension handles JFR configurations
    @Override
    public boolean isEnabledFor(RunConfigurationBase configuration, ProfilerConfigurationState state) {
        return JFR_CONFIGURATION_TYPE_ID.equals(state.getConfigurationTypeId());
    }

    // consulo: upstream inherits patch() (updateJavaParameters) from JFRConfigurationExtensionBase
    @Override
    public void patch(RunConfigurationBase configuration, ProfilerConfigurationState state, ProfilerLaunchContext context) {
        // TODO consulo: JfrProfilerConfigurationState, JfrOptions and JvmRecordingFiles are not exported by consulo.java.profiler.impl
        // if (!(state instanceof JfrProfilerConfigurationState jfrState)) {
        //     return;
        // }
        // List<String> vmOptions = context.getVmOptions();
        // if (vmOptions == null) {
        //     return;
        // }
        // Path recordingFile = JvmRecordingFiles.create("launch");
        // vmOptions.addAll(JfrOptions.launchVmOptions(jfrState, recordingFile, featureVersion, Platform.current().os().isLinux()));
        // context.putUserData(RECORDING_FILE, recordingFile);
    }

    // consulo: upstream attachToProcess(profilerConfigurationState, profilerParameters: RecordingConfiguration, runConfiguration, handler);
    // openToolWindowAndReportStart(process, runConfiguration) is done by the program runner with the returned process
    // (ProfilerToolWindowManager.addProfilerProcessTab)
    @Override
    public @Nullable ProfilerProcess<?> attachToProcess(
        RunConfigurationBase configuration,
        ProcessHandler handler,
        ProfilerConfigurationState state,
        ProfilerLaunchContext context
    ) {
        // consulo: upstream `if (handler !is OSProcessHandler) return`; OSProcessHandler is internal in Consulo, the process needs an
        // OS process id instead
        if (handler.getId() <= 0) {
            return null;
        }

        // TODO consulo: NewProcessStartedWithProfilerCommunicator + JFRProfilerProcess -> JvmProfilerProcess.launched(), not exported
        //  by consulo.java.profiler.impl
        // int pid = (int) handler.getId();
        // return JvmProfilerProcess.launched(
        //     configuration.getProject(),
        //     concurrency,
        //     new JvmTargetProcess(configuration.getName() + " (" + pid + ")", pid),
        //     jfrState,
        //     handler,
        //     context.getUserData(RECORDING_FILE)
        // );
        return null;
    }
}
