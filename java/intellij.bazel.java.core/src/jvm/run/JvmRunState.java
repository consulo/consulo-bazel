package org.jetbrains.bazel.jvm.run;

import consulo.bazel.execution.ui.SettingsEditorFragment;
import consulo.bazel.execution.ui.SettingsEditorFragmentContainer;
import consulo.project.Project;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jdom.Element;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.AbstractGenericRunState;
import org.jetbrains.bazel.run.state.Common;
import org.jetbrains.bazel.run.state.RunWithBazel;

import java.util.Collection;

// consulo: upstream @com.intellij.configurationStore.Property(description = ...) annotations have no Consulo counterpart
public class JvmRunState
    extends AbstractGenericRunState<JvmRunState>
    implements HasDebugPort, RunWithBazel.HasRunWithBazel {
    private int myDebugPort = 5005;

    private boolean myRunWithBazel;

    public JvmRunState(Project project) {
        myRunWithBazel = ProjectViewExtensions.getRunConfigRunWithBazel(ProjectViewService.projectView(project));
    }

    // consulo: XmlSerializer (SkipDefaultsSerializationFilter) needs a no-arg instance for the default values
    public JvmRunState() {
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
            JvmTestState.addDebugPortFragment(container);
            container.add(Common.bazelParamsFragment());
            container.add(Common.programArgumentsFragment());
            Common.addEnvironmentFragment(container);
            container.add(new RunWithBazel.RunWithBazelFragment());
        });
    }
}
