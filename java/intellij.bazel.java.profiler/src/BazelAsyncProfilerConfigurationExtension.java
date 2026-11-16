package com.intellij.bazel.java.profiler;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.process.ProcessHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

// TODO consulo: upstream extends com.intellij.profiler.ultimate.async.AsyncProfilerConfigurationExtensionBase. Neither the platform
//  nor consulo-java has an async-profiler integration (consulo-java profiles with JFR only; its consulo.java.profiler.jfr module only
//  reads JFR files with async-profiler's reader): no async-profiler configuration type, AgentConfiguration, AsyncProfilerProcess or
//  NewProcessStartedWithProfilerCommunicator. The class is kept without the base type (it would implement the platform
//  ProfilerConfigurationExtension) and is not registered (upstream: <runConfigurationExtension>)
public class BazelAsyncProfilerConfigurationExtension {
    public BazelAsyncProfilerConfigurationExtension() {
    }

    // consulo: upstream `override`
    public boolean isApplicableFor(RunConfigurationBase configuration) {
        // TODO consulo: super.isApplicableFor(configuration) of AsyncProfilerConfigurationExtensionBase
        return configuration instanceof BazelRunConfiguration;
    }

    // consulo: upstream attachToProcess(profilerConfigurationState, profilerParameters: AgentConfiguration, runConfiguration, handler),
    // mapped onto ProfilerConfigurationExtension.attachToProcess(); openToolWindowAndReportStart(process, runConfiguration) is done by
    // the program runner with the returned process (ProfilerToolWindowManager.addProfilerProcessTab)
    public @Nullable ProfilerProcess<?> attachToProcess(
        RunConfigurationBase runConfiguration,
        ProcessHandler handler,
        ProfilerConfigurationState profilerConfigurationState
    ) {
        // consulo: upstream `if (handler !is OSProcessHandler) return`; OSProcessHandler is internal in Consulo, the process needs an
        // OS process id instead
        if (handler.getId() <= 0) {
            return null;
        }

        // TODO consulo: async-profiler is missing
        // NewProcessStartedWithProfilerCommunicator communicator =
        //     new NewProcessStartedWithProfilerCommunicator(runConfiguration.getName(), handler);
        // return new AsyncProfilerProcess(
        //     runConfiguration.getProject(),
        //     communicator,
        //     profilerConfigurationState,
        //     profilerParameters,
        //     System.currentTimeMillis()
        // );
        return null;
    }
}
