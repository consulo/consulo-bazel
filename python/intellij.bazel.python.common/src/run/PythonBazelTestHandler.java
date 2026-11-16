package org.jetbrains.bazel.python.run;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.run.BazelCommandLineStateBase;
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState;
import org.jetbrains.bazel.run.state.AbstractGenericTestState;
import org.jetbrains.bazel.run.state.GenericTestState;
import org.jetbrains.bsp.protocol.TestParams;

import java.util.LinkedHashMap;
import java.util.Map;

// consulo: the nested upstream PythonBazelTestHandler.Provider has its own file (nested @ExtensionImpl classes are not supported by
// the annotation processor), see PythonBazelTestHandlerProvider
public class PythonBazelTestHandler extends PythonBazelHandler<GenericTestState> {
    private final GenericTestState myState = new GenericTestState();

    @Override
    public String getName() {
        return BazelPluginLocalize.pythonTestHandlerName().get();
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public GenericTestState getState() {
        return myState;
    }

    @Override
    protected BazelCommandLineStateBase createCommandLineState(
        ExecutionEnvironment environment
    ) {
        return new BazelPythonTestCommandLineState(environment, myState);
    }

    public static TestParams addJunitXmlOptionsToEnvironment(TestParams testParams) {
        Map<String, String> envs = new LinkedHashMap<>(testParams.environmentVariables());
        String originalPytestOpts = envs.get("PYTEST_ADDOPTS");
        if (originalPytestOpts == null) {
            envs.put("PYTEST_ADDOPTS", "--junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1");
        }
        else if (!originalPytestOpts.contains("junitxml")) {
            envs.put("PYTEST_ADDOPTS", originalPytestOpts + " --junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1");
        }
        else {
            // if the user has already specified --junitxml, we don't want to interfere with that
            return testParams;
        }
        // consulo: data class copy(environmentVariables = envs)
        return new TestParams(
            testParams.taskId(),
            testParams.targets(),
            testParams.arguments(),
            envs,
            testParams.useCoverage(),
            testParams.coverageInstrumentationFilter(),
            testParams.testFilter(),
            testParams.additionalBazelParams(),
            testParams.streamTestOutput()
        );
    }
}

// consulo: upstream private class of PythonBazelTestHandler.kt
class BazelPythonTestCommandLineState extends BazelTestCommandLineState {
    BazelPythonTestCommandLineState(
        ExecutionEnvironment environment,
        AbstractGenericTestState<?> state
    ) {
        super(environment, state);
    }

    @Override
    protected TestParams transformTestParams(TestParams params) {
        return PythonBazelTestHandler.addJunitXmlOptionsToEnvironment(params);
    }
}
