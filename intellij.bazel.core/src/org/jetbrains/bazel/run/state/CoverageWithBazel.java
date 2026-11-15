package org.jetbrains.bazel.run.state;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.CommonParameterFragments;
import consulo.bazel.execution.ui.NestedGroupFragment;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.JBTextField;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

// consulo: upstream Kotlin top-level declarations of CoverageWithBazel.kt; the interface and the fragment are nested here
public final class CoverageWithBazel {
    private CoverageWithBazel() {
    }

    public interface HasCoverageInstrumentationFilter {
        @Nullable String getCoverageInstrumentationFilter();

        void setCoverageInstrumentationFilter(@Nullable String coverageInstrumentationFilter);
    }

    public static class CoverageWithBazelFragment extends NestedGroupFragment<BazelRunConfiguration> {
        private BooleanSupplier myIsAvailableWhen = () -> true;

        public CoverageWithBazelFragment() {
            super(
                "bazelCoverage",
                BazelPluginLocalize.runconfigCoverageName(),
                BazelPluginLocalize.runconfigCoverageGroup(),
                configuration -> false
            );
        }

        @Override
        protected List<SettingsEditorFragment<BazelRunConfiguration, ?>> createChildren() {
            return List.of(coverageInstrumentationFilterFragment());
        }

        @Override
        public boolean isAvailable() {
            return myIsAvailableWhen.getAsBoolean() && super.isAvailable();
        }

        public void availableWhen(BooleanSupplier condition) {
            myIsAvailableWhen = condition;
        }
    }

    private static SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JBTextField>>
    coverageInstrumentationFilterFragment() {
        JBTextField filterField = new JBTextField();
        CommonParameterFragments.setMonospaced(filterField);
        LocalizeValue message = BazelPluginLocalize.runconfigCoverageInstrumentationFilter();
        filterField.getAccessibleContext().setAccessibleName(message.get());
        filterField.getEmptyText().setText(BazelPluginLocalize.runconfigCoverageInstrumentationFilterEmptyText());
        SettingsEditorLabeledComponent<JBTextField> labeled = new SettingsEditorLabeledComponent<>(message, filterField);
        SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JBTextField>> fragment =
            new SettingsEditorFragment<>(
                "bazelCoverageInstrumentationFilter",
                message,
                BazelPluginLocalize.runconfigCoverageGroup(),
                labeled,
                (configuration, component) -> {
                    HasCoverageInstrumentationFilter state = getCoverageInstrumentationFilterState(configuration);
                    String filter = state == null ? null : state.getCoverageInstrumentationFilter();
                    component.getComponent().setText(filter == null ? "" : filter);
                },
                (configuration, component) -> {
                    HasCoverageInstrumentationFilter state = getCoverageInstrumentationFilterState(configuration);
                    if (state != null) {
                        String text = component.getComponent().getText();
                        state.setCoverageInstrumentationFilter(text.isBlank() ? null : text);
                    }
                },
                configuration -> {
                    HasCoverageInstrumentationFilter state = getCoverageInstrumentationFilterState(configuration);
                    String filter = state == null ? null : state.getCoverageInstrumentationFilter();
                    return filter != null && !filter.isBlank();
                }
            );
        fragment.setHint(BazelPluginLocalize.runconfigCoverageInstrumentationFilterHint());
        return fragment;
    }

    private static @Nullable HasCoverageInstrumentationFilter getCoverageInstrumentationFilterState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasCoverageInstrumentationFilter filterState ? filterState : null;
    }
}
