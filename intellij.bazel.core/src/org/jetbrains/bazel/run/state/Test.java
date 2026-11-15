package org.jetbrains.bazel.run.state;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.CommonParameterFragments;
import consulo.bazel.execution.ui.LabeledSettingsFragmentInfo;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.bazel.execution.ui.SettingsEditorFragmentType;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

// consulo: upstream Kotlin top-level declarations of Test.kt; the interface is nested here
public final class Test {
    private Test() {
    }

    public interface HasTestFilter {
        @Nullable String getTestFilter();

        void setTestFilter(@Nullable String testFilter);
    }

    private static @Nullable HasTestFilter getTestFilterState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasTestFilter hasTestFilter ? hasTestFilter : null;
    }

    public static SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JTextField>> addTestFilterFragment(
        SettingsEditorFragmentContainer<BazelRunConfiguration> receiver
    ) {
        return receiver.addLabeledSettingsEditorFragment(
            new LabeledSettingsFragmentInfo() {
                @Override
                public LocalizeValue getSettingsActionHint() {
                    return LocalizeValue.empty();
                }

                @Override
                public LocalizeValue getSettingsGroup() {
                    // consulo: upstream uses this id-like string as the group name
                    return LocalizeValue.of("bsp.fragment.testFilter");
                }

                @Override
                public LocalizeValue getSettingsHint() {
                    return LocalizeValue.empty();
                }

                @Override
                public String getSettingsId() {
                    return "Test Filter ID";
                }

                @Override
                public LocalizeValue getSettingsName() {
                    // TODO consulo: move to the BazelPluginLocalize YAML (upstream hard-codes it)
                    return LocalizeValue.localizeTODO("Test Filter");
                }

                @Override
                public LocalizeValue getEditorLabel() {
                    return BazelPluginLocalize.settingsEditorLabelTestFilter();
                }

                @Override
                public SettingsEditorFragmentType getSettingsType() {
                    return SettingsEditorFragmentType.EDITOR;
                }
            },
            disposable -> {
                JTextField textField = new JTextField();
                CommonParameterFragments.setMonospaced(textField);
                return textField;
            },
            (configuration, component) -> {
                HasTestFilter state = getTestFilterState(configuration);
                String testFilter = state == null ? null : state.getTestFilter();
                component.setText(testFilter == null ? "" : testFilter);
            },
            (configuration, component) -> {
                HasTestFilter state = getTestFilterState(configuration);
                if (state != null) {
                    state.setTestFilter(component.getText());
                }
            }
        );
    }
}
