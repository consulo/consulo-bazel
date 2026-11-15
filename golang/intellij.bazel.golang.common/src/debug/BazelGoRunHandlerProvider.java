package org.jetbrains.bazel.golang.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTarget;

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
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new BazelGoRunHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTarget> targets) {
        for (BuildTarget it : targets) {
            if (!(GoBazelRules.includesGo(it.getKind()) && it.getKind().ruleType() == RuleType.BINARY)) {
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
