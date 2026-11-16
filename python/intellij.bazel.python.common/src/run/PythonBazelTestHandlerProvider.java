package org.jetbrains.bazel.python.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.python.lang.PythonLanguageClass;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.import_.GooglePluginAwareRunHandlerProvider;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

// consulo: upstream PythonBazelTestHandler.Provider (nested in PythonBazelTestHandler.kt); own file because nested @ExtensionImpl
// classes are not supported by the annotation processor
@ExtensionImpl(id = "PythonBazelTestHandlerProvider")
public class PythonBazelTestHandlerProvider implements GooglePluginAwareRunHandlerProvider {
    @Override
    public String getId() {
        return "PythonBazelTestHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelPluginLocalize.pythonTestHandlerName();
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new PythonBazelTestHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        for (BuildTargetInfo it : targets) {
            if (!(it.kind().languageClasses().contains(PythonLanguageClass.PYTHON) && it.kind().ruleType() == RuleType.TEST)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getGoogleHandlerId() {
        return "BlazePyTestConfigurationHandlerProvider";
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }
}
