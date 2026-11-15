package org.jetbrains.bazel.run.state;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentType;
import consulo.ui.ex.awt.FormBuilder;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

// consulo: upstream Kotlin top-level declarations of RunWithBazel.kt; the interface and the fragment are nested here
public final class RunWithBazel {
    private RunWithBazel() {
    }

    public interface HasRunWithBazel {
        boolean getRunWithBazel();

        void setRunWithBazel(boolean runWithBazel);
    }

    // consulo: upstream component is a Kotlin UI DSL DialogPanel, here a FormBuilder panel (the context help is a tooltip row)
    public static class RunWithBazelFragment extends SettingsEditorFragment<BazelRunConfiguration, JPanel> {
        private final JCheckBox myCheckBox;

        public RunWithBazelFragment(JCheckBox checkBox) {
            super(
                "Run with Bazel",
                BazelPluginLocalize.runconfigRunWithBazel(),
                BazelPluginLocalize.runconfigRunWithBazel(),
                FormBuilder.createFormBuilder()
                    .addComponent(checkBox)
                    .addTooltip(BazelPluginLocalize.runconfigRunWithBazelHint().get())
                    .getPanel(),
                /* priority = */ 1,
                SettingsEditorFragmentType.EDITOR,
                (configuration, component) -> {
                    HasRunWithBazel state = getRunWithBazelState(configuration);
                    checkBox.setSelected(state != null && state.getRunWithBazel());
                },
                (configuration, component) -> {
                    HasRunWithBazel state = getRunWithBazelState(configuration);
                    if (state != null) {
                        state.setRunWithBazel(checkBox.isSelected());
                    }
                },
                configuration -> true
            );
            myCheckBox = checkBox;
            setRemovable(false);
        }

        public RunWithBazelFragment() {
            this(new JCheckBox(BazelPluginLocalize.runconfigRunWithBazel().get()));
        }

        public JCheckBox getCheckBox() {
            return myCheckBox;
        }
    }

    private static @Nullable HasRunWithBazel getRunWithBazelState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasRunWithBazel hasRunWithBazel ? hasRunWithBazel : null;
    }
}
