package org.jetbrains.bazel.jvm.run;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.LabeledSettingsFragmentInfo;
import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.bazel.execution.ui.SettingsEditorFragmentType;
import consulo.bazel.execution.ui.SettingsEditorLabeledComponent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jdom.Element;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.AbstractGenericTestState;
import org.jetbrains.bazel.run.state.Common;
import org.jetbrains.bazel.run.state.CoverageWithBazel;
import org.jetbrains.bazel.run.state.RunWithBazel;
import org.jetbrains.bazel.run.state.Test;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

// consulo: upstream @com.intellij.configurationStore.Property(description = ...) annotations have no Consulo counterpart
public class JvmTestState
    extends AbstractGenericTestState<JvmTestState>
    implements HasDebugPort, RunWithBazel.HasRunWithBazel {
    private int myDebugPort = 5005;

    private boolean myRunWithBazel;

    public JvmTestState(Project project) {
        myRunWithBazel = ProjectViewExtensions.getRunConfigRunWithBazel(ProjectViewService.projectView(project));
    }

    // consulo: XmlSerializer (SkipDefaultsSerializationFilter) needs a no-arg instance for the default values
    public JvmTestState() {
    }

    // consulo: upstream `property(project.projectView().runConfigRunWithBazel)` skips the project view value as the default;
    // SkipDefaultsSerializationFilter compares with the no-arg instance instead, so runWithBazel is always written
    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute("runWithBazel", String.valueOf(myRunWithBazel));
    }

    @Attribute("debugPort")
    @Override
    public int getDebugPort() {
        return myDebugPort;
    }

    @Override
    public void setDebugPort(int debugPort) {
        myDebugPort = debugPort;
    }

    @Attribute("runWithBazel")
    @Override
    public boolean getRunWithBazel() {
        return myRunWithBazel;
    }

    @Override
    public void setRunWithBazel(boolean runWithBazel) {
        myRunWithBazel = runWithBazel;
    }

    @Override
    public Collection<SettingsEditorFragment<BazelRunConfiguration, ?>> createFragments(BazelRunConfiguration configuration) {
        return SettingsEditorFragmentContainer.<BazelRunConfiguration>fragments(container -> {
            addDebugPortFragment(container);
            container.add(Common.bazelParamsFragment());
            Test.addTestFilterFragment(container);
            container.add(Common.programArgumentsFragment());
            Common.addEnvironmentFragment(container);
            container.add(new RunWithBazel.RunWithBazelFragment());
            container.add(new CoverageWithBazel.CoverageWithBazelFragment());
        });
    }

    private static @Nullable HasDebugPort getDebugPortState(BazelRunConfiguration receiver) {
        BazelRunHandler handler = receiver.getHandler();
        return handler != null && handler.getState() instanceof HasDebugPort hasDebugPort ? hasDebugPort : null;
    }

    // consulo: upstream internal top-level extension fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addDebugPortFragment()
    public static SettingsEditorFragment<BazelRunConfiguration, SettingsEditorLabeledComponent<JSpinner>> addDebugPortFragment(
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
                    return LocalizeValue.of("bazel.fragment.debugPort");
                }

                @Override
                public LocalizeValue getSettingsHint() {
                    return LocalizeValue.empty();
                }

                @Override
                public String getSettingsId() {
                    return "Debug Port ID";
                }

                @Override
                public LocalizeValue getSettingsName() {
                    // TODO consulo: move to the localize YAML (upstream hard-codes it)
                    return LocalizeValue.localizeTODO("Debug Port");
                }

                @Override
                public LocalizeValue getEditorLabel() {
                    return BazelPluginLocalize.debugEditorLabel();
                }

                @Override
                public SettingsEditorFragmentType getSettingsType() {
                    return SettingsEditorFragmentType.EDITOR;
                }
            },
            disposable -> {
                // consulo: com.intellij.ui.PortField is ide-impl in Consulo; the same JSpinner setup (0..65535, "#")
                JSpinner portField = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
                portField.setEditor(new JSpinner.NumberEditor(portField, "#"));
                return portField;
            },
            (configuration, component) -> {
                HasDebugPort state = getDebugPortState(configuration);
                component.setValue(state == null ? 0 : state.getDebugPort());
            },
            (configuration, component) -> {
                HasDebugPort state = getDebugPortState(configuration);
                if (state != null) {
                    state.setDebugPort(((SpinnerNumberModel) component.getModel()).getNumber().intValue());
                }
            }
        );
    }
}

// consulo: upstream internal top-level interface of JvmTestState.kt, also implemented by JvmRunState
interface HasDebugPort {
    int getDebugPort();

    void setDebugPort(int debugPort);
}
