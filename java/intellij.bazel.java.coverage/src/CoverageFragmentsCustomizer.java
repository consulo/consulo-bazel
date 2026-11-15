package org.jetbrains.bazel.java.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationEditorCustomizer;
import org.jetbrains.bazel.run.config.TargetsFragment;
import org.jetbrains.bazel.run.state.CoverageWithBazel;
import org.jetbrains.bazel.run.state.RunWithBazel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Switches between native IntelliJ and Bazel coverage UI.
 * It depends on the {@link BazelJavaAgentCoverageEngine#isJavaAgentCoverageApplicableTo} result.
 */
@ExtensionImpl
public class CoverageFragmentsCustomizer implements BazelRunConfigurationEditorCustomizer {

    @Override
    public void fragmentsCreated(
        BazelRunConfiguration configuration,
        List<SettingsEditorFragment<BazelRunConfiguration, ?>> fragments
    ) {
        RunWithBazel.RunWithBazelFragment runWithBazel = findFragment(fragments, RunWithBazel.RunWithBazelFragment.class);
        if (runWithBazel == null) {
            return;
        }
        CoverageWithBazel.CoverageWithBazelFragment bazelCoverage =
            findFragment(fragments, CoverageWithBazel.CoverageWithBazelFragment.class);
        if (bazelCoverage == null) {
            return;
        }
        TargetsFragment targets = findFragment(fragments, TargetsFragment.class);
        if (targets == null) {
            return;
        }
        BooleanSupplier useJavaAgentCoverage =
            () -> BazelJavaAgentCoverageEngine.isJavaAgentCoverageApplicableTo(
                targets.getTargets(),
                runWithBazel.getCheckBox().isSelected()
            );
        bazelCoverage.availableWhen(() -> !useJavaAgentCoverage.getAsBoolean());
        runWithBazel.getCheckBox().addItemListener(e -> selectCoverageUi(fragments, useJavaAgentCoverage.getAsBoolean()));
        targets.addTargetsListener(() -> selectCoverageUi(fragments, useJavaAgentCoverage.getAsBoolean()));
    }

    @Override
    public void fragmentsReset(
        BazelRunConfiguration configuration,
        List<SettingsEditorFragment<BazelRunConfiguration, ?>> fragments
    ) {
        selectCoverageUi(fragments, BazelJavaAgentCoverageEngine.isJavaAgentCoverageApplicableTo(configuration));
    }

    // TODO consulo: com.intellij.execution.configurations.coverage.CoverageFragment is missing: the Java coverage settings of a Consulo
    //  run configuration are separate fragments of the coverage engine (shown when BazelJavaAgentCoverageEngine is applicable), and the
    //  fragment editor bridge has no selected/unselected fragments, so there is nothing to switch
    @SuppressWarnings("unused")
    private void selectCoverageUi(List<SettingsEditorFragment<BazelRunConfiguration, ?>> fragments, boolean useJavaCoverage) {
        boolean useBazelCoverage = !useJavaCoverage;
        // val javaGroup = fragments.findFragment<CoverageFragment<BazelRunConfiguration>>() ?: return
        CoverageWithBazel.CoverageWithBazelFragment bazelGroup = findFragment(fragments, CoverageWithBazel.CoverageWithBazelFragment.class);
        if (bazelGroup == null) {
            return;
        }
        // if (javaGroup.component() == null || bazelGroup.component() == null) return
        // val javaChildren = javaGroup.children
        // val bazelChildren = bazelGroup.children
        // if (bazelChildren.all { it.isSelected == useBazelCoverage } && javaChildren.all { it.isSelected == useJavaCoverage }) return
        // bazelChildren.forEach { it.isSelected = useBazelCoverage }
        // javaChildren.forEach { it.isSelected = useJavaCoverage }
    }

    // consulo: upstream private inline reified extension fun List<SettingsEditorFragment<BazelRunConfiguration, *>>.findFragment<T>()
    private static <T> @Nullable T findFragment(List<SettingsEditorFragment<BazelRunConfiguration, ?>> receiver, Class<T> type) {
        for (SettingsEditorFragment<BazelRunConfiguration, ?> it : receiver) {
            if (type.isInstance(it)) {
                return type.cast(it);
            }
        }
        return null;
    }
}
