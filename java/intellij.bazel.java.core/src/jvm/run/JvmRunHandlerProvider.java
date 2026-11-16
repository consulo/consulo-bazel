package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

// consulo: upstream JvmRunHandler.JvmRunHandlerProvider (nested in JvmRunHandler.kt); own file because nested @ExtensionImpl classes
// are not supported by the annotation processor
@ExtensionImpl(id = "JvmRunHandlerProvider")
public class JvmRunHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "JvmRunHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelPluginLocalize.jvmRunHandlerName();
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new JvmRunHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        for (BuildTargetInfo it : targets) {
            if (!(JavaLanguageClass.isJvmTarget(it.kind()) && it.kind().ruleType() != RuleType.TEST)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeJavaRunConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }
}
