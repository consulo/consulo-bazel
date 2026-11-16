package org.jetbrains.bazel.run.handler;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

@ExtensionImpl(order = "last")
public class GenericRunHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "GenericRunHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelPluginLocalize.genericRunHandlerName();
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new GenericBazelRunHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        return targets.size() == 1 && targets.get(0).kind().ruleType() == RuleType.BINARY;
    }

    @Override
    public boolean canRunNonImported(Project project, List<Label> targets) {
        return targets.size() == 1 && !(targets.get(0).target() instanceof Label.AllRuleTargets);
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeCommandGenericRunConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }
}
