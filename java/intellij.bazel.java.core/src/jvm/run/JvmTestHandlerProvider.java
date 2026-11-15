package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.List;

// consulo: upstream JvmTestHandler.JvmTestHandlerProvider (nested in JvmTestHandler.kt); own file because nested @ExtensionImpl
// classes are not supported by the annotation processor
@ExtensionImpl(id = "JvmTestHandlerProvider")
public class JvmTestHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "JvmTestHandlerProvider";
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new JvmTestHandler(configuration);
    }

    @Override
    public boolean canRun(Project project, List<BuildTarget> targets) {
        for (BuildTarget it : targets) {
            if (!(JavaLanguageClass.isJvmTarget(it.getKind()) && it.getKind().ruleType() == RuleType.TEST)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canRunNonImported(Project project, List<Label> targets) {
        // If a custom extension accepts those targets, then it follows it can run them (even though they aren't imported)
        return !(JvmTestRunnerExtension.getInstance(project, targets) instanceof DefaultJvmTestRunnerExtension);
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazeJavaRunConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }
}
