package org.jetbrains.bazel.debug.configuration;

import consulo.execution.configuration.CommandLineState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import org.jetbrains.bazel.run.BazelProcessHandler;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugResult;

import java.util.concurrent.CompletableFuture;

public class StarlarkDebugConfigurationState extends CommandLineState {
    private final Project myProject;
    private final String myTarget;

    // allows passing execution results (both successful and exceptional) to BspProcessHandler from outside
    private final CompletableFuture<AnalysisDebugResult> myFutureProxy = new CompletableFuture<>();

    public StarlarkDebugConfigurationState(
        Project project,
        String target,
        ExecutionEnvironment environment
    ) {
        super(environment);
        myProject = project;
        myTarget = target;
    }

    public Project getProject() {
        return myProject;
    }

    public String getTarget() {
        return myTarget;
    }

    public CompletableFuture<AnalysisDebugResult> getFutureProxy() {
        return myFutureProxy;
    }

    @Override
    protected BazelProcessHandler startProcess() {
        BazelProcessHandler handler = new BazelProcessHandler(myProject, myFutureProxy);
        handler.startNotify();
        return handler;
    }
}
