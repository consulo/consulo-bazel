package org.jetbrains.bazel.java.coverage;

import com.intellij.java.coverage.JavaCoverageEnabledConfiguration;
import com.intellij.java.execution.impl.RunConfigurationExtension;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunnerSettings;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.ui.gutters.BazelRunLocation;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ExtensionImpl
public class BazelJavaRunConfigurationExtension extends RunConfigurationExtension {
    @Override
    public <T extends RunConfigurationBase> void updateJavaParameters(
        T configuration,
        OwnJavaParameters params,
        @Nullable RunnerSettings runnerSettings
    ) {
    }

    @Override
    protected boolean isApplicableFor(RunConfigurationBase configuration) {
        return BazelJavaAgentCoverageEngine.isJavaAgentCoverageApplicableTo(configuration);
    }

    /**
     * See {@code com.intellij.execution.coverage.CoverageJavaRunConfigurationExtension.extendCreatedConfiguration}
     * which assumes a configuration implements {@code com.intellij.execution.CommonJavaRunConfigurationParameters}
     */
    @Override
    protected void extendCreatedConfiguration(RunConfigurationBase configuration, Location location) {
        JavaCoverageEnabledConfiguration coverageEnabledConfiguration = JavaCoverageEnabledConfiguration.getFrom(configuration);
        if (coverageEnabledConfiguration == null) {
            return;
        }

        if (!(location instanceof BazelRunLocation bazelRunLocation)) {
            return;
        }
        PsiElement psiElement = bazelRunLocation.getOriginalLocation().getPsiElement();
        if (psiElement == null) {
            return;
        }
        String testClassFqn = null;
        for (FullyQualifiedNameProvider extension : FullyQualifiedNameProvider.getEp()) {
            testClassFqn = ReadAction.nonBlocking(() -> extension.getFullyQualifiedName(psiElement)).executeSynchronously();
            if (testClassFqn != null) {
                break;
            }
        }
        if (testClassFqn == null) {
            return;
        }

        // By default, only consider classes having the same package as the test class
        coverageEnabledConfiguration.setUpCoverageFilters(testClassFqn, null);
    }

    // consulo: upstream @ApiStatus.Internal public top-level interface of BazelJavaRunConfigurationExtension.kt
    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface FullyQualifiedNameProvider {
        // consulo: upstream @RequiresReadLock(generateAssertion = false /* IJPL-115548 */)
        @RequiredReadAction
        @Nullable String getFullyQualifiedName(PsiElement psiElement);

        // consulo: upstream `ep`, served by the Consulo extension point
        static List<FullyQualifiedNameProvider> getEp() {
            return Application.get().getExtensionList(FullyQualifiedNameProvider.class);
        }
    }
}

@ExtensionImpl
class JavaFullyQualifiedNameProvider implements BazelJavaRunConfigurationExtension.FullyQualifiedNameProvider {
    public JavaFullyQualifiedNameProvider() {
    }

    @RequiredReadAction
    @Override
    public @Nullable String getFullyQualifiedName(PsiElement psiElement) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
        if (psiClass == null) {
            return null;
        }
        return psiClass.getQualifiedName();
    }
}
