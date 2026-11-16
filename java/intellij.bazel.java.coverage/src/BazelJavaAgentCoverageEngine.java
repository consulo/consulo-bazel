package org.jetbrains.bazel.java.coverage;

import com.intellij.java.coverage.IDEACoverageRunner;
import com.intellij.java.coverage.JavaCoverageEngine;
import com.intellij.java.coverage.JavaCoverageOptionsProvider;
import com.intellij.java.coverage.JavaCoverageSuite;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.jvm.run.JvmTestHandler;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * https://youtrack.jetbrains.com/issue/BAZEL-3130
 * bazel coverage requires a correctly set {@code --instrumentation_filter}.
 * Using IDEA's native Java coverage (by using {@code -javaagent}) also gives us class/method/branch coverage,
 * not just per-line coverage.
 * <p>
 * Only enabled if {@code run_config_run_with_bazel: false} is in the {@code .projectview}
 *
 * @see #isJavaAgentCoverageApplicableTo
 */
@ExtensionImpl(order = "before bazelCoverageEngine")
public class BazelJavaAgentCoverageEngine extends JavaCoverageEngine {
    @Override
    public boolean isApplicableTo(@Nullable RunConfigurationBase configuration) {
        return isJavaAgentCoverageApplicableTo(configuration);
    }

    @Override
    public @Nullable CoverageSuite createEmptyCoverageSuite(CoverageRunner coverageRunner) {
        return new BazelJavaCoverageSuite(this);
    }

    // consulo: the Consulo createSuite() has no excludePatterns and calls branchCoverage "tracingEnabled"
    @Override
    protected JavaCoverageSuite createSuite(
        CoverageRunner acceptedCovRunner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        String[] filters,
        long lastCoverageTimeStamp,
        boolean coverageByTestEnabled,
        boolean branchCoverage,
        boolean trackTestFolders,
        Project project
    ) {
        return new BazelJavaCoverageSuite(
            name,
            coverageDataFileProvider,
            filters,
            /* excludePatterns = */ null,
            lastCoverageTimeStamp,
            coverageByTestEnabled,
            branchCoverage,
            trackTestFolders,
            acceptedCovRunner,
            this,
            project
        );
    }

    /**
     * See {@link BazelJavaCoverageSuite}, we don't want the platform to iterate output jars,
     * so {@link JavaCoverageEngine#recompileProjectAndRerunAction} will complain about it via a notification. Disable it here
     */
    @Override
    public boolean recompileProjectAndRerunAction(Module module, CoverageSuitesBundle suite, Runnable chooseSuiteAction) {
        return false;
    }

    // Use IDEACoverageRunner instead of the default JaCoco, because JaCoCoReportLoader depends on collectOutputRoots.
    // However, we don't want the platform to iterate output jars, see BazelJavaCoverageSuite for more explanation.
    private CoverageRunner getCoverageRunner() {
        return CoverageRunner.getInstance(IDEACoverageRunner.class);
    }

    @Override
    public CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nullable RunConfigurationBase conf) {
        CoverageEnabledConfiguration configuration = super.createCoverageEnabledConfiguration(conf);
        configuration.setCoverageRunner(getCoverageRunner());
        return configuration;
    }

    // consulo: upstream overrides createCoverageSuite(config); Consulo's CoverageDataManager calls this variant with the config
    @Override
    public @Nullable CoverageSuite createCoverageSuite(
        CoverageRunner covRunner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        CoverageEnabledConfiguration config
    ) {
        JavaCoverageOptionsProvider.getInstance(config.getConfiguration().getProject()).setCoverageRunner(getCoverageRunner());
        return super.createCoverageSuite(covRunner, name, coverageDataFileProvider, config);
    }

    // consulo: upstream internal top-level functions of BazelJavaAgentCoverageEngine.kt

    /**
     * @see BazelJavaAgentCoverageEngine
     */
    public static boolean isJavaAgentCoverageApplicableTo(@Nullable RunProfile configuration) {
        if (!(configuration instanceof BazelRunConfiguration bazelRunConfiguration)) {
            return false;
        }
        if (!(bazelRunConfiguration.getHandler() instanceof JvmTestHandler handler)) {
            return false;
        }
        return isJavaAgentCoverageApplicableTo(bazelRunConfiguration.getTargets(), handler.getState().getRunWithBazel());
    }

    public static boolean isJavaAgentCoverageApplicableTo(List<Label> targets, boolean runWithBazel) {
        // 1. Only one concrete target can be run with --script_path
        // 2. Vanilla "bazel coverage" works with remote execution,
        //    but BazelJavaAgentCoverageEngine doesn't as it uses -javaagent with a jar file from the local system.
        //    So here we give the choice to the user
        return targets.size() == 1 && !targets.get(0).isWildcard() && !runWithBazel;
    }
}
