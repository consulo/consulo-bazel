package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.go.localize.BazelGoLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

// consulo: upstream BazelGoRunHandler.BazelGoRunHandlerProvider (nested in BazelGoRunHandler.kt); own file because nested
// @ExtensionImpl classes are not supported by the annotation processor
@ExtensionImpl(id = "BazelGoRunHandlerProvider")
public class BazelGoRunHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "BazelGoRunHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelGoLocalize.goRunHandlerName();
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new BazelGoRunHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        for (BuildTargetInfo it : targets) {
            if (!(GoBazelRules.includesGo(it.kind()) && it.kind().ruleType() == RuleType.BINARY)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeGoRunConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }
}
