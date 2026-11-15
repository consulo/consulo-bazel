package org.jetbrains.bazel.run;

import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.action.AbstractRerunFailedTestsAction;
import consulo.execution.test.sm.ui.SMTRunnerConsoleView;
import consulo.module.Module;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;

class BazelRerunFailedTestsAction extends AbstractRerunFailedTestsAction {
    private final BazelTestRerunExtension myTestRerunExtension;

    BazelRerunFailedTestsAction(SMTRunnerConsoleView consoleView, BazelTestRerunExtension testRerunExtension) {
        super(consoleView.getConsole());
        myTestRerunExtension = testRerunExtension;
        init(consoleView.getProperties());
        setModelProvider(() -> consoleView.getResultsViewer());
    }

    @Override
    protected @Nullable MyRunProfile getRunProfile(ExecutionEnvironment environment) {
        if (!(myConsoleProperties.getConfiguration() instanceof BazelRunConfiguration original)
            || !(original.clone() instanceof BazelRunConfiguration configuration)) {
            return null;
        }
        List<AbstractTestProxy> failedTests = getFailedTests(configuration.getProject());
        if (!myTestRerunExtension.setTestsToRerun(configuration, failedLeafTests(failedTests))) {
            return null;
        }
        return new MyRunProfile(configuration) {
            // consulo: MyRunProfile does not implement getModules(), a Bazel run configuration has no modules
            @Override
            public Module[] getModules() {
                return Module.EMPTY_ARRAY;
            }

            @Override
            public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
                // environment.runProfile is AbstractRerunFailedTestsAction$MyRunProfile here, so we have to pass the configuration manually
                environment.putUserData(BazelRunConfiguration.BAZEL_RUN_CONFIGURATION_KEY, configuration);
                return configuration.getState(executor, environment);
            }
        };
    }

    public static List<AbstractTestProxy> failedLeafTests(List<AbstractTestProxy> failedTests) {
        return failedTests.stream().filter(AbstractTestProxy::isLeaf).toList();
    }
}
