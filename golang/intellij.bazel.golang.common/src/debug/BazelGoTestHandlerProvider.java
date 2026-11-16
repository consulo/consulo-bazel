package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.go.localize.BazelGoLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

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
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelGoLocalize.goTestHandlerName();
    }

    @Override
    public BazelGoTestHandler createRunHandler(BazelRunConfiguration configuration) {
        return new BazelGoTestHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        for (BuildTargetInfo it : targets) {
            if (!(GoBazelRules.includesGo(it.kind()) && it.kind().ruleType() == RuleType.TEST)) {
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
        return true;
    }
}
