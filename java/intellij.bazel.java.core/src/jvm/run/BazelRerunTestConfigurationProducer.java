package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.test.AbstractTestProxy;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.run.BazelTestRerunExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Allows right-clicking on a test in the test results and then rerunning it separately from other tests
 *
 * @see org.jetbrains.bazel.run.BazelRerunFailedTestsAction
 * @see com.intellij.execution.junit.UniqueIdConfigurationProducer
 */
// consulo: LazyRunConfigurationProducer -> RunConfigurationProducer
@ExtensionImpl
public class BazelRerunTestConfigurationProducer extends RunConfigurationProducer<BazelRunConfiguration> {
    public BazelRerunTestConfigurationProducer() {
        super(BazelRunConfigurationType.getBazelRunConfigurationFactory());
    }

    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return BazelRunConfigurationType.getBazelRunConfigurationFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context,
        SimpleReference<PsiElement> sourceElement
    ) {
        BazelTestRerunExtension extension = BazelTestRerunExtension.getInstance(configuration);
        if (extension == null || !extension.setTestsToRerun(configuration, getTestsFromTestConsole(context))) {
            return false;
        }

        AbstractTestProxy selectedProxy = context.getDataContext().getData(AbstractTestProxy.KEY);
        if (selectedProxy != null) {
            String configurationName = getConfigurationName(selectedProxy);
            if (configurationName != null) {
                configuration.setName(configurationName);
            }
        }

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context
    ) {
        BazelTestRerunExtension extension = BazelTestRerunExtension.getInstance(configuration);
        return extension != null && extension.isRerunOf(configuration, getTestsFromTestConsole(context));
    }

    private List<AbstractTestProxy> getTestsFromTestConsole(ConfigurationContext context) {
        AbstractTestProxy[] proxies = context.getDataContext().getData(AbstractTestProxy.KEY_OF_ARRAY);
        return proxies == null ? List.of() : List.of(proxies);
    }

    private @Nullable String getConfigurationName(AbstractTestProxy proxy) {
        // For a URL like java:test://com.example.TestClass/testMethod we will return testMethod
        String locationUrl = proxy.getLocationUrl();
        if (locationUrl == null) {
            return proxy.getName();
        }
        int slashIndex = locationUrl.lastIndexOf('/');
        if (slashIndex == -1) {
            return proxy.getName();
        }
        return locationUrl.substring(slashIndex + 1);
    }
}
