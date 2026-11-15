package org.jetbrains.bazel.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.execution.test.AbstractTestProxy;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.state.Test;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelTestRerunExtension {
    boolean isApplicable(BazelRunConfiguration configuration);

    boolean setTestsToRerun(BazelRunConfiguration configuration, List<AbstractTestProxy> tests);

    boolean isRerunOf(BazelRunConfiguration configuration, List<AbstractTestProxy> tests);

    static @Nullable BazelTestRerunExtension getInstance(BazelRunConfiguration configuration) {
        return Application.get().getExtensionPoint(BazelTestRerunExtension.class).findFirstSafe(it -> it.isApplicable(configuration));
    }

    // consulo: upstream declares this class as a top-level declaration in the same file
    abstract class BazelTestFilterRerunExtension implements BazelTestRerunExtension {
        @Override
        public final boolean setTestsToRerun(BazelRunConfiguration configuration, List<AbstractTestProxy> tests) {
            BazelRunHandler handler = configuration.getHandler();
            if (handler == null) {
                return false;
            }
            BazelRunConfigurationState<?> state = handler.getState();
            // consulo: like Kotlin `(state as? HasTestFilter)?.testFilter = ... ?: return false`, the filter is computed only for a
            // HasTestFilter state
            if (state instanceof Test.HasTestFilter hasTestFilter) {
                String testFilter = getTestFilter(configuration, tests);
                if (testFilter == null) {
                    return false;
                }
                hasTestFilter.setTestFilter(testFilter);
            }
            return true;
        }

        @Override
        public final boolean isRerunOf(BazelRunConfiguration configuration, List<AbstractTestProxy> tests) {
            BazelRunHandler handler = configuration.getHandler();
            if (handler == null) {
                return false;
            }
            BazelRunConfigurationState<?> state = handler.getState();
            String testFilter = getTestFilter(configuration, tests);
            if (testFilter == null) {
                return false;
            }
            return Objects.equals(testFilter, state instanceof Test.HasTestFilter hasTestFilter ? hasTestFilter.getTestFilter() : null);
        }

        public abstract @Nullable String getTestFilter(BazelRunConfiguration configuration, List<AbstractTestProxy> tests);
    }
}
