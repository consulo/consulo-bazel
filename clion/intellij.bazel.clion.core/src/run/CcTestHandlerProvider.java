package org.jetbrains.bazel.clion.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags;
import org.jetbrains.bazel.clion.sync.CcLanguageClass;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

// consulo: upstream CcTestHandler.Provider (nested in CcRunHandler.kt); own file because nested @ExtensionImpl classes are not
// supported by the annotation processor
@ExtensionImpl(id = "CcTestHandlerProvider")
public class CcTestHandlerProvider implements RunHandlerProvider {
    @Override
    public String getId() {
        return "CcTestHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelCLionCoreLocalize.testHandlerCcName();
    }

    @Override
    public boolean canBeForced() {
        return BazelCLionFeatureFlags.isCLionEnabled();
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new CcRunHandler.CcTestHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        if (!BazelCLionFeatureFlags.isCLionEnabled()) {
            return false;
        }
        for (BuildTargetInfo it : targets) {
            if (!(CcLanguageClass.includesCc(it.kind()) && it.kind().ruleType() == RuleType.TEST)) {
                return false;
            }
        }
        return true;
    }
}
