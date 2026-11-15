package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.List;

// consulo: upstream BazelGoTestHandler.BazelGoTestHandlerProvider (nested in BazelGoTestHandler.kt); own file because nested
// @ExtensionImpl classes are not supported by the annotation processor
@ExtensionImpl(id = "BazelGoTestHandlerProvider")
public class BazelGoTestHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "BazelGoTestHandlerProvider";
    }

    @Override
    public BazelGoTestHandler createRunHandler(BazelRunConfiguration configuration) {
        return new BazelGoTestHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTarget> targets) {
        for (BuildTarget it : targets) {
            if (!(GoBazelRules.includesGo(it.getKind()) && it.getKind().ruleType() == RuleType.TEST)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeGoTestConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }
}
