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

// consulo: upstream CcRunHandler.Provider (nested in CcRunHandler.kt); own file because nested @ExtensionImpl classes are not supported
// by the annotation processor
@ExtensionImpl(id = "CcRunHandlerProvider")
public class CcRunHandlerProvider implements RunHandlerProvider {
    @Override
    public String getId() {
        return "CcRunHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelCLionCoreLocalize.runHandlerCcName();
    }

    @Override
    public boolean canBeForced() {
        return BazelCLionFeatureFlags.isCLionEnabled();
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new CcRunHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        if (!BazelCLionFeatureFlags.isCLionEnabled()) {
            return false;
        }

        if (targets.size() != 1) {
            return false;
        }
        BuildTargetInfo target = targets.get(0);
        return CcLanguageClass.includesCc(target.kind()) && target.kind().ruleType() == RuleType.BINARY;
    }
}
