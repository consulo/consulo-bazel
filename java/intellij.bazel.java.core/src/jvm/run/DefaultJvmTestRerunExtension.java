package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.AbstractTestProxy;
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer;
import org.jetbrains.bazel.run.BazelTestRerunExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.testing.java.BazelJavaTestLocationHintProvider;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl(order = "last")
public class DefaultJvmTestRerunExtension extends BazelTestRerunExtension.BazelTestFilterRerunExtension {
    @Override
    public boolean isApplicable(BazelRunConfiguration configuration) {
        return configuration.getHandler() instanceof JvmTestHandler;
    }

    @Override
    public @Nullable String getTestFilter(BazelRunConfiguration configuration, List<AbstractTestProxy> tests) {
        return jvmTestFilterFromTests(tests);
    }

    // consulo: upstream top-level functions of DefaultJvmTestRerunExtension.kt

    public static @Nullable String jvmTestFilterFromTests(List<AbstractTestProxy> tests) {
        Set<String> filters = new LinkedHashSet<>();
        for (AbstractTestProxy it : tests) {
            String locationUrl = it.getLocationUrl();
            String filter = locationUrl == null ? null : jvmTestFilterFromLocationUrl(locationUrl);
            if (filter != null) {
                filters.add(filter);
            }
        }
        String result = String.join("|", filters);
        return result.isEmpty() ? null : result;
    }

    /**
     * Reads back a location hint written by {@link BazelJavaTestLocationHintProvider},
     * e.g., {@code java:suite://com.example.Outer$Inner} or {@code java:test://com.example.Outer$Inner/testName}, and turns it into a test filter.
     *
     * @see org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer#getGutterAction
     */
    public static @Nullable String jvmTestFilterFromLocationUrl(String locationUrl) {
        String body;
        if (locationUrl.startsWith(BazelJavaTestLocationHintProvider.TEST_SUITE_PREFIX)) {
            body = locationUrl.substring(BazelJavaTestLocationHintProvider.TEST_SUITE_PREFIX.length());
        }
        else if (locationUrl.startsWith(BazelJavaTestLocationHintProvider.TEST_CASE_PREFIX)) {
            body = locationUrl.substring(BazelJavaTestLocationHintProvider.TEST_CASE_PREFIX.length());
        }
        else {
            return null;
        }
        String[] classAndMethod = body.split("/", -1);
        return BazelJavaRunConfigurationProducer.getTestFilter(
            /* className = */ classAndMethod[0],
            /* methodName = */ classAndMethod.length > 1 ? classAndMethod[1] : null
        );
    }
}
