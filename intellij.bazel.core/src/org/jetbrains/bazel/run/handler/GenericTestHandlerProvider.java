package org.jetbrains.bazel.run.handler;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.List;

@ExtensionImpl(order = "last")
public class GenericTestHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "GenericTestHandlerProvider";
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new GenericBazelTestHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTarget> targets) {
        for (BuildTarget it : targets) {
            if (it.getKind().ruleType() != RuleType.TEST) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canRunNonImported(Project project, List<Label> targets) {
        return true;
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeCommandGenericRunConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }
}
