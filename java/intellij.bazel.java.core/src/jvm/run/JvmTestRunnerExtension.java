package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.project.Project;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bazel.run.task.BazelTestTaskListener;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TestParams;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface JvmTestRunnerExtension {
    boolean isApplicable(Project project, List<Label> targets);

    boolean isIdBasedTestTree();

    boolean getTestRunnerEmitsServiceMessages();

    TestParams transformTestParams(TestParams params);

    Pair<Map<String, String>, @Nullable String> transformScriptPathEnvAndTestFilter(Map<String, String> env, @Nullable String testFilter);

    BazelTaskListener createTaskListener(BazelProcessHandler handler, @Nullable Consumer<Path> coverageReportListener);

    // consulo: upstream `ep`, served by the Consulo extension point
    static List<JvmTestRunnerExtension> getEp() {
        return Application.get().getExtensionList(JvmTestRunnerExtension.class);
    }

    static JvmTestRunnerExtension getInstance(Project project, List<Label> targets) {
        for (JvmTestRunnerExtension it : getEp()) {
            if (it.isApplicable(project, targets)) {
                return it;
            }
        }
        throw new NoSuchElementException("Collection contains no element matching the predicate.");
    }
}

@ExtensionImpl(order = "last")
class DefaultJvmTestRunnerExtension implements JvmTestRunnerExtension {
    public DefaultJvmTestRunnerExtension() {
    }

    @Override
    public boolean isApplicable(Project project, List<Label> targets) {
        return true;
    }

    @Override
    public boolean isIdBasedTestTree() {
        return false;
    }

    @Override
    public boolean getTestRunnerEmitsServiceMessages() {
        return false;
    }

    @Override
    public TestParams transformTestParams(TestParams params) {
        return params;
    }

    @Override
    public Pair<Map<String, String>, @Nullable String> transformScriptPathEnvAndTestFilter(
        Map<String, String> env,
        @Nullable String testFilter
    ) {
        return Pair.create(env, testFilter);
    }

    @Override
    public BazelTaskListener createTaskListener(BazelProcessHandler handler, @Nullable Consumer<Path> coverageReportListener) {
        return new BazelTestTaskListener(handler, coverageReportListener);
    }
}
