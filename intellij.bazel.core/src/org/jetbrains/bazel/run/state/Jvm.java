package org.jetbrains.bazel.run.state;

import consulo.bazel.execution.ui.CommandLinePanel;
import consulo.bazel.execution.ui.CommonParameterFragments;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

// consulo: upstream Kotlin top-level declarations of Jvm.kt; the interface is nested here
public final class Jvm {
    private Jvm() {
    }

    public interface HasJavaVmOptions {
        @Nullable String getJavaVmOptions();

        void setJavaVmOptions(@Nullable String javaVmOptions);
    }

    private static @Nullable HasJavaVmOptions getJavaVmOptionsState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasJavaVmOptions hasJavaVmOptions ? hasJavaVmOptions : null;
    }

    public static SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> vmOptions() {
        // consulo: ExecutionBundle group.java.options is missing in ExecutionLocalize
        LocalizeValue group = LocalizeValue.localizeTODO("Java");
        RawCommandLineEditor vmOptions = new RawCommandLineEditor();
        CommandLinePanel.setMinimumWidth(vmOptions, 400);
        CommonParameterFragments.setMonospaced(vmOptions.getTextField());
        // consulo: ExecutionBundle run.configuration.java.vm.parameters.empty.text is missing in ExecutionLocalize
        LocalizeValue message = LocalizeValue.localizeTODO("VM options");
        vmOptions.getTextField().getAccessibleContext().setAccessibleName(message.get());
        // TODO consulo: RawCommandLineEditor has no editor field with an empty text (placeholder)
        // vmOptions.editorField.emptyText.text = message
        // TODO consulo: MacrosDialog.addMacroSupport(vmOptions.editorField, MacrosDialog.Filters.ALL) { false } - MacrosDialog
        //  is not an API in Consulo
        // TODO consulo: TextComponentEmptyText.setupPlaceholderVisibility(vmOptions.editorField)
        SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> vmParameters =
            new SettingsEditorFragment<>(
                "vmParameters",
                // consulo: ExecutionBundle run.configuration.java.vm.parameters.name is missing in ExecutionLocalize
                LocalizeValue.localizeTODO("Add &VM options"),
                group,
                vmOptions,
                15,
                (configuration, component) -> {
                    HasJavaVmOptions state = getJavaVmOptionsState(configuration);
                    component.setText(state == null ? null : state.getJavaVmOptions());
                },
                (configuration, component) -> {
                    HasJavaVmOptions state = getJavaVmOptionsState(configuration);
                    if (state != null) {
                        state.setJavaVmOptions(component.isVisible() ? component.getText() : null);
                    }
                },
                configuration -> {
                    HasJavaVmOptions state = getJavaVmOptionsState(configuration);
                    return StringUtil.isNotEmpty(state == null ? null : state.getJavaVmOptions());
                }
            );
        // consulo: ExecutionBundle run.configuration.java.vm.parameters.hint is missing in ExecutionLocalize
        vmParameters.setHint(LocalizeValue.localizeTODO("VM options. CLI arguments to the ‘Java’ command. Example: -ea -Xmx2048m"));
        // consulo: ExecutionBundle specify.vm.options.for.running.the.application is missing in ExecutionLocalize
        vmParameters.setActionHint(LocalizeValue.localizeTODO("Specify VM options for running the application"));
        // consulo: RawCommandLineEditor has no getEditorField(), the text field is the editor
        vmParameters.setEditorGetter((RawCommandLineEditor editor) -> editor.getTextField());

        return vmParameters;
    }
}
