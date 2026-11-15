package org.jetbrains.bazel.run.state;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.CommandLinePanel;
import consulo.bazel.execution.ui.CommonParameterFragments;
import consulo.bazel.execution.ui.EnvironmentFragments;
import consulo.bazel.execution.ui.LabeledSettingsFragmentInfo;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// consulo: upstream Kotlin top-level declarations of Common.kt; public interfaces and classes are nested here
public final class Common {
    private Common() {
    }

    public interface HasEnv {
        EnvironmentVariablesDataOptions getEnv();

        void setEnv(EnvironmentVariablesDataOptions env);
    }

    private static @Nullable HasEnv getEnvState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasEnv hasEnv ? hasEnv : null;
    }

    // Copied from org.jetbrains.plugins.terminal.EnvironmentVariablesDataOptions
    // We can't use it directly because it comes from a different plugin
    // consulo: upstream is a BaseState, here a plain bean for XmlSerializer
    @Tag("")
    public static class EnvironmentVariablesDataOptions {
        private Map<String, String> myEnvs = new LinkedHashMap<>();

        private boolean myPassParentEnvs = true;

        // user order of env must be preserved - do not sort user input
        // consulo: upstream @com.intellij.configurationStore.Property(description = "Environment variables") has no Consulo counterpart
        @Property(surroundWithTag = false)
        @MapAnnotation(
            surroundWithTag = false,
            surroundKeyWithTag = false,
            surroundValueWithTag = false,
            entryTagName = "env",
            keyAttributeName = "key",
            sortBeforeSave = false
        )
        public Map<String, String> getEnvs() {
            return myEnvs;
        }

        // consulo: setter for XmlSerializer (upstream property is read-only)
        public void setEnvs(Map<String, String> envs) {
            myEnvs = new LinkedHashMap<>(envs);
        }

        public boolean isPassParentEnvs() {
            return myPassParentEnvs;
        }

        public void setPassParentEnvs(boolean passParentEnvs) {
            myPassParentEnvs = passParentEnvs;
        }

        public void set(EnvironmentVariablesData envData) {
            myEnvs.clear();
            myEnvs.putAll(envData.getEnvs());
            setPassParentEnvs(envData.isPassParentEnvs());
            // consulo: BaseState.incrementModificationCount() has no counterpart in a plain bean
        }

        public EnvironmentVariablesData get() {
            return EnvironmentVariablesData.create(myEnvs, isPassParentEnvs());
        }
    }

    @RequiredUIAccess
    public static SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JComponent>> addEnvironmentFragment(
        SettingsEditorFragmentContainer<BazelRunConfiguration> receiver
    ) {
        SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JComponent>> fragment =
            EnvironmentFragments.addEnvironmentFragment(
                receiver,
                new LabeledSettingsFragmentInfo() {
                    @Override
                    public LocalizeValue getEditorLabel() {
                        return ExecutionLocalize.environmentVariablesComponentTitle();
                    }

                    @Override
                    public String getSettingsId() {
                        return "external.system.environment.variables.fragment";
                    }

                    @Override
                    public LocalizeValue getSettingsName() {
                        // consulo: ExecutionBundle environment.variables.fragment.name
                        return ExecutionLocalize.runConfigurationFragmentEnvironment();
                    }

                    @Override
                    public LocalizeValue getSettingsGroup() {
                        // consulo: ExecutionBundle group.operating.system
                        return ExecutionLocalize.runConfigurationFragmentGroupOperatingSystem();
                    }

                    @Override
                    public LocalizeValue getSettingsHint() {
                        // consulo: ExecutionBundle environment.variables.fragment.hint is missing in ExecutionLocalize
                        return LocalizeValue.localizeTODO("Separate variables with semicolon: VAR=value; VAR1=value1");
                    }

                    @Override
                    public LocalizeValue getSettingsActionHint() {
                        // consulo: ExecutionBundle set.custom.environment.variables.for.the.process is missing in ExecutionLocalize
                        return LocalizeValue.localizeTODO("Set custom environment variables for the process");
                    }
                },
                it -> {
                    HasEnv envState = getEnvState(it);
                    return envState == null ? Map.of() : envState.getEnv().getEnvs();
                },
                (it, envs) -> {
                    HasEnv envState = getEnvState(it);
                    if (envState == null) {
                        return;
                    }
                    EnvironmentVariablesDataOptions env = envState.getEnv();
                    env.set(env.get().with(envs));
                },
                it -> {
                    HasEnv envState = getEnvState(it);
                    return envState != null && envState.getEnv().isPassParentEnvs();
                },
                (it, passParentEnvs) -> {
                    HasEnv envState = getEnvState(it);
                    if (envState != null) {
                        envState.getEnv().setPassParentEnvs(passParentEnvs);
                    }
                },
                /* hideWhenEmpty = */ false
            );
        fragment.setValidation(configuration -> {
            // When running via --script_path (instead of bazel run), we only pass envs to the program itself, not to Bazel:
            // bazel run --script_path=foo //foo && export FOO=BAR ./foo
            HasEnv envState = getEnvState(configuration);
            BazelRunHandler handler = configuration.getHandler();
            if (envState != null && !envState.getEnv().getEnvs().isEmpty()
                && (handler == null
                || handler.getState() instanceof RunWithBazel.HasRunWithBazel hasRunWithBazel && hasRunWithBazel.getRunWithBazel())) {
                return List.of(
                    new ValidationInfo(BazelPluginLocalize.runconfigBazelWarningsEnvVars(), fragment.getEditorComponent()).asWarning()
                );
            }
            else {
                return List.of();
            }
        });
        return fragment;
    }

    public interface HasProgramArguments {
        @Nullable String getProgramArguments();

        void setProgramArguments(@Nullable String programArguments);
    }

    private static @Nullable HasProgramArguments getProgramArgumentsState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasProgramArguments hasProgramArguments ? hasProgramArguments : null;
    }

    public static SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> programArgumentsFragment() {
        RawCommandLineEditor programArguments = new RawCommandLineEditor();
        CommandLinePanel.setMinimumWidth(programArguments, 400);
        // consulo: ExecutionBundle run.configuration.program.parameters.placeholder ("Program arguments")
        LocalizeValue message = ExecutionLocalize.runConfigurationFragmentProgramParameters();
        // TODO consulo: RawCommandLineEditor has no editor field with an empty text (placeholder)
        // programArguments.editorField.emptyText.text = message
        programArguments.getTextField().getAccessibleContext().setAccessibleName(message.get());
        // TODO consulo: TextComponentEmptyText.setupPlaceholderVisibility(programArguments.editorField)
        CommonParameterFragments.setMonospaced(programArguments.getTextField());
        // TODO consulo: MacrosDialog.addMacroSupport(programArguments.editorField, MacrosDialog.Filters.ALL) { false } - MacrosDialog
        //  is not an API in Consulo
        SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> parameters =
            new SettingsEditorFragment<>(
                "commandLineParameters",
                // consulo: ExecutionBundle run.configuration.program.parameters.name
                ExecutionLocalize.runConfigurationFragmentProgramParameters(),
                null,
                programArguments,
                100,
                (configuration, component) -> {
                    HasProgramArguments state = getProgramArgumentsState(configuration);
                    component.setText(state == null ? null : state.getProgramArguments());
                },
                (configuration, component) -> {
                    HasProgramArguments state = getProgramArgumentsState(configuration);
                    if (state != null) {
                        state.setProgramArguments(component.getText());
                    }
                },
                configuration -> true
            );
        parameters.setRemovable(false);
        // consulo: RawCommandLineEditor has no getEditorField(), the text field is the editor
        parameters.setEditorGetter((RawCommandLineEditor editor) -> editor.getTextField());
        // consulo: ExecutionBundle run.configuration.program.parameters.hint is missing in ExecutionLocalize
        parameters.setHint(LocalizeValue.localizeTODO("CLI arguments to your application"));

        return parameters;
    }

    public interface HasBazelParams {
        @Nullable String getAdditionalBazelParams();

        void setAdditionalBazelParams(@Nullable String additionalBazelParams);
    }

    private static @Nullable HasBazelParams getBazelParamsState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasBazelParams hasBazelParams ? hasBazelParams : null;
    }

    public static SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> bazelParamsFragment() {
        RawCommandLineEditor bazelParams = new RawCommandLineEditor();
        CommandLinePanel.setMinimumWidth(bazelParams, 400);
        LocalizeValue message = BazelPluginLocalize.runconfigBazelFlags();
        // TODO consulo: RawCommandLineEditor has no editor field with an empty text (placeholder)
        // bazelParams.editorField.emptyText.text = message
        bazelParams.getTextField().getAccessibleContext().setAccessibleName(message.get());
        // TODO consulo: TextComponentEmptyText.setupPlaceholderVisibility(bazelParams.editorField)
        CommonParameterFragments.setMonospaced(bazelParams.getTextField());
        // TODO consulo: MacrosDialog.addMacroSupport(bazelParams.editorField, MacrosDialog.Filters.ALL) { false } - MacrosDialog
        //  is not an API in Consulo
        SettingsEditorFragment<BazelRunConfiguration, RawCommandLineEditor> parameters =
            new SettingsEditorFragment<>(
                "bazelParameters",
                BazelPluginLocalize.runconfigBazelFlags(),
                null,
                bazelParams,
                100,
                (configuration, component) -> {
                    HasBazelParams state = getBazelParamsState(configuration);
                    String additionalBazelParams = state == null ? null : state.getAdditionalBazelParams();
                    component.setText(additionalBazelParams == null ? "" : additionalBazelParams);
                },
                (configuration, component) -> {
                    HasBazelParams state = getBazelParamsState(configuration);
                    if (state != null) {
                        state.setAdditionalBazelParams(component.getText());
                    }
                },
                configuration -> true
            );
        parameters.setRemovable(false);
        // consulo: RawCommandLineEditor has no getEditorField(), the text field is the editor
        parameters.setEditorGetter((RawCommandLineEditor editor) -> editor.getTextField());
        parameters.setHint(BazelPluginLocalize.runconfigBazelFlags());
        parameters.setValidation(configuration -> {
            HasBazelParams state = getBazelParamsState(configuration);
            String additionalBazelParams = state == null ? null : state.getAdditionalBazelParams();
            if (additionalBazelParams != null && !additionalBazelParams.isEmpty()) {
                return List.of(
                    new ValidationInfo(BazelPluginLocalize.runconfigBazelWarningsBazelFlags(), bazelParams.getTextField()).asWarning()
                );
            }
            else {
                return List.of();
            }
        });

        return parameters;
    }
}
