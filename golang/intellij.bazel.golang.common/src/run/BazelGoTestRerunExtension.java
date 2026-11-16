package org.jetbrains.bazel.golang.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.AbstractTestProxy;
import org.jetbrains.bazel.golang.debug.BazelGoTestHandler;
import org.jetbrains.bazel.run.BazelTestRerunExtension.BazelTestFilterRerunExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ExtensionImpl
public class BazelGoTestRerunExtension extends BazelTestFilterRerunExtension {
    @Override
    public boolean isApplicable(BazelRunConfiguration configuration) {
        return configuration.getHandler() instanceof BazelGoTestHandler;
    }

    @Override
    public @Nullable String getTestFilter(BazelRunConfiguration configuration, List<AbstractTestProxy> tests) {
        String testFilter = tests.stream()
            .map(AbstractTestProxy::getLocationUrl)
            .filter(Objects::nonNull)
            .map(BazelGoTestRerunExtension::getTestName)
            .filter(Objects::nonNull)
            .distinct()
            .map(BazelGoRunConfigurationProducer::regexifyTestFilter)
            .collect(Collectors.joining("|"));
        return !testFilter.isEmpty() ? testFilter : null;
    }

    /**
     * @see BazelGoTestLocationHintProvider
     */
    private static @Nullable String getTestName(String locationUrl) {
        // consulo: Kotlin substringAfter('#', "").substringBefore('/')
        int hashIndex = locationUrl.indexOf('#');
        String afterHash = hashIndex != -1 ? locationUrl.substring(hashIndex + 1) : "";
        int slashIndex = afterHash.indexOf('/');
        String testName = slashIndex != -1 ? afterHash.substring(0, slashIndex) : afterHash;
        return !testName.isEmpty() ? testName : null;
    }
}
