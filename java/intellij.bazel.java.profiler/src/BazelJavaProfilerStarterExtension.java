package com.intellij.bazel.java.profiler;

import consulo.execution.configuration.RunProfile;
import org.jetbrains.bazel.jvm.run.JvmRunHandler;
import org.jetbrains.bazel.jvm.run.JvmTestHandler;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;

// TODO consulo: upstream extends com.intellij.profiler.ultimate.JavaProfilerStarterExtension (extension point
//  profiler.javaProfilerStarterExtension). consulo-java's counterpart, the ProfilerStarter of the JFR configuration type
//  (consulo.java.profiler.impl.internal.jfr.JavaProfilerStarter), accepts only CommonJavaRunConfigurationParameters and has no
//  extension point; the class is kept without the base type and is not registered
public class BazelJavaProfilerStarterExtension {
    public BazelJavaProfilerStarterExtension() {
    }

    // consulo: upstream `override`
    public boolean canRun(RunProfile profile) {
        if (!(profile instanceof BazelRunConfiguration configuration)) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return (handler instanceof JvmRunHandler || handler instanceof JvmTestHandler) && configuration.getTargets().size() == 1;
    }
}
