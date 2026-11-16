package org.jetbrains.bazel.clion.workspace.environment;

import consulo.logging.Logger;
import consulo.process.cmd.GeneralCommandLine;
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jspecify.annotations.Nullable;

import java.util.Map;

// consulo: upstream top-level declarations of BazelEnvironmentProvider.kt
public final class BazelEnvironmentProvider {
    private BazelEnvironmentProvider() {
    }

    private static final Logger LOG = Logger.getInstance(BazelEnvironmentProvider.class);
    private static final String MSVC_TOOLCHAIN_NAME = "synthetic_msvc_bazel";
    private static final String CLANG_CL_TOOLCHAIN_NAME = "synthetic_clang_cl_bazel";

    /**
     * Creates and configures the environment for a specific compiler. At the moment, only the MSVC compiler requires a
     * custom configuration.
     */
    public static CcToolEnvironment createToolEnvironment(CcCompilerInfo settings) {
        if (!settings.cCompilerKind().equals(settings.cppCompilerKind())) {
            LOG.warn("C and Cpp compiler mismatch. Defaulting to Cpp compiler.");
        }

        @Nullable CcToolEnvironment environment;
        if (settings.cppCompilerKind().equals(CcCompilerKind.MSVC)) {
            environment = createMsvcEnvironment(settings);
        }
        else if (settings.cppCompilerKind().equals(CcCompilerKind.CLANG_CL)) {
            environment = createClangClEnvironment(settings);
        }
        else {
            environment = null;
        }

        return environment != null ? environment : createToolEnvironment(settings.environment());
    }

    private static @Nullable CcToolEnvironment createMsvcEnvironment(CcCompilerInfo settings) {
        String toolSetPath = MSVCEnvironmentUtil.getToolSetPath(settings.cppCompiler());
        if (toolSetPath == null) {
            LOG.warn("Could not find tool set path.");
            return null;
        }

        // TODO consulo: CLion's MSVC toolchain API (MSVCCompilerToVersionCache, CPPToolchains.Toolchain, CPPToolSet.Kind.MSVC,
        //  MSVC.setToolsVersion, CPPEnvironment) does not exist in consulo-cpp; the plain environment is used instead
        //  val version = MSVCCompilerToVersionCache.getInstance().getCompilerVersion(settings.cppCompiler.toAbsolutePath().toString())
        //  if (version == null) {
        //    LOG.warn("Could not derive arch and version.")
        //    return null
        //  }
        //  val toolchain = CPPToolchains.Toolchain(OS.CURRENT)
        //  toolchain.setName(MSVC_TOOLCHAIN_NAME)
        //  toolchain.setToolSetKind(CPPToolSet.Kind.MSVC)
        //  toolchain.setToolSetPath(toolSetPath)
        //  val environment = createToolEnvironment(settings, toolchain)
        //  (environment.toolSet as MSVC).setToolsVersion(version)
        //  return environment
        return null;
    }

    private static @Nullable CcToolEnvironment createClangClEnvironment(CcCompilerInfo settings) {
        String toolSetPath = MSVCEnvironmentUtil.getToolSetPath(null);
        if (toolSetPath == null) {
            LOG.warn("Could not find tool set path.");
            return null;
        }

        // TODO consulo: CLion's MSVC toolchain API (CPPToolchains.Toolchain, CPPToolSet.Kind.MSVC, CPPEnvironment) does not
        //  exist in consulo-cpp; the plain environment is used instead
        //  val toolchain = CPPToolchains.Toolchain(OS.CURRENT)
        //  toolchain.setName(CLANG_CL_TOOLCHAIN_NAME)
        //  toolchain.setToolSetKind(CPPToolSet.Kind.MSVC)
        //  toolchain.setToolSetPath(toolSetPath)
        //  return createToolEnvironment(settings, toolchain)
        return null;
    }

    // TODO consulo: CPPEnvironment / CPPToolchains.Toolchain do not exist in consulo-cpp
    //  /**
    //   * Creates a CPPEnvironment that forwards the environment variables set on the Bazel compiler settings.
    //   */
    //  private fun createToolEnvironment(settings: CcCompilerInfo, toolchain: CPPToolchains.Toolchain): CPPEnvironment {
    //    return object : CPPEnvironment(toolchain) {
    //      override fun prepare(cl: GeneralCommandLine, prepareFor: PrepareFor) {
    //        super.prepare(cl, prepareFor)
    //        cl.environment.putAll(settings.environment)
    //      }
    //    }
    //  }

    public static CcToolEnvironment createToolEnvironment(Map<String, String> environment) {
        // consulo: upstream returns a plain CidrToolEnvironment for an empty environment; CcToolEnvironment with an
        // empty map behaves the same
        return new CcToolEnvironment(environment);
    }

    /**
     * consulo: stands for CIDR's {@code CidrToolEnvironment} (upstream creates anonymous subclasses that add the
     * environment), which does not exist in consulo-cpp.
     * <p>
     * TODO consulo: CIDR {@code CidrToolEnvironment} (the environment the compiler resolver and the compiler info
     * collection run the compiler in) does not exist in consulo-cpp; this class only adds the toolchain environment to a
     * command line.
     */
    public static class CcToolEnvironment {
        private final Map<String, String> myEnvironment;

        public CcToolEnvironment(Map<String, String> environment) {
            myEnvironment = environment;
        }

        public Map<String, String> getEnvironment() {
            return myEnvironment;
        }

        public void prepare(GeneralCommandLine commandLine) {
            // TODO consulo: super.prepare(commandLine, prepareFor) of CidrToolEnvironment
            commandLine.getEnvironment().putAll(myEnvironment);
        }
    }
}
