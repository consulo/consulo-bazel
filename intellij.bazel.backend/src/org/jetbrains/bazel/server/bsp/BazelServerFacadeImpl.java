package org.jetbrains.bazel.server.bsp;

import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.ModuleResolver;
import org.jetbrains.bazel.bazelrunner.ModuleResolver.ResolvedModulesAndWarning;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.server.BazelQuery;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryParams;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryResult;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.server.model.BazelSyncProject.AspectSyncProject;
import org.jetbrains.bazel.server.model.BazelSyncProject.PhasedSyncProject;
import org.jetbrains.bazel.server.sync.BspProjectMapper;
import org.jetbrains.bazel.server.sync.ExecuteService;
import org.jetbrains.bazel.server.sync.ProjectResolver;
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.CompileParams;
import org.jetbrains.bsp.protocol.CompileResult;
import org.jetbrains.bsp.protocol.JvmToolchainInfo;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.RunResult;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugParams;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugResult;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TestParams;
import org.jetbrains.bsp.protocol.TestResult;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams.WorkspaceBuildTargetSelector;
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams;
import org.jetbrains.bsp.protocol.WorkspaceDirectories.WorkspaceDirectoriesResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.server.bzlmod.RepoMapping.calculateRepoMapping;

public class BazelServerFacadeImpl implements BazelServerFacade {
    private final BspProjectMapper myBspMapper;
    private final ProjectResolver myProjectResolver;
    private final FirstPhaseProjectResolver myFirstPhaseProjectResolver;
    private final ExecuteService myExecuteService;
    private final BazelRunner myBazelRunner;
    private final BazelTaskEventsHandler myTaskEventsHandler;
    private final ProjectView myProjectView;
    private final BazelInfo myBazelInfo;
    private final BazelPathsResolver myBazelPathsResolver;
    private final BazelOutFileHardLinks myOutFileHardLinks;
    private final OutputLocationResolver myOutputResolver;
    private final OutputLocationParser myOutputParser;

    public BazelServerFacadeImpl(
        BspProjectMapper bspMapper,
        ProjectResolver projectResolver,
        FirstPhaseProjectResolver firstPhaseProjectResolver,
        ExecuteService executeService,
        BazelRunner bazelRunner,
        BazelTaskEventsHandler taskEventsHandler,
        ProjectView projectView,
        BazelInfo bazelInfo,
        BazelPathsResolver bazelPathsResolver,
        BazelOutFileHardLinks outFileHardLinks,
        OutputLocationResolver outputResolver,
        OutputLocationParser outputParser
    ) {
        myBspMapper = bspMapper;
        myProjectResolver = projectResolver;
        myFirstPhaseProjectResolver = firstPhaseProjectResolver;
        myExecuteService = executeService;
        myBazelRunner = bazelRunner;
        myTaskEventsHandler = taskEventsHandler;
        myProjectView = projectView;
        myBazelInfo = bazelInfo;
        myBazelPathsResolver = bazelPathsResolver;
        myOutFileHardLinks = outFileHardLinks;
        myOutputResolver = outputResolver;
        myOutputParser = outputParser;
    }

    @Override
    public ProjectView getProjectView() {
        return myProjectView;
    }

    @Override
    public BazelInfo getBazelInfo() {
        return myBazelInfo;
    }

    @Override
    public BazelPathsResolver getBazelPathsResolver() {
        return myBazelPathsResolver;
    }

    @Override
    public BazelOutFileHardLinks getOutFileHardLinks() {
        return myOutFileHardLinks;
    }

    @Override
    public OutputLocationResolver getOutputResolver() {
        return myOutputResolver;
    }

    @Override
    public OutputLocationParser getOutputParser() {
        return myOutputParser;
    }

    @Override
    public CompletableFuture<RepoMapping> workspaceRepoMapping(TaskId taskId) {
        // consulo: telemetry is not ported, upstream ran this in the "Calculating external repository mapping" bspTracer span
        return calculateRepoMapping(myProjectView, myBazelRunner, myBazelInfo, BazelTaskEventsHandler.asLogger(myTaskEventsHandler, taskId), taskId);
    }

    @Override
    public CompletableFuture<ResolvedModulesAndWarning> showRepos(List<String> repoNames, TaskId taskId) {
        return new ModuleResolver(myBazelRunner, myProjectView, taskId).resolveModules(repoNames, myBazelInfo);
    }

    @Override
    public CompletableFuture<AspectSyncProject> workspaceBuildTargets(WorkspaceBuildTargetParams params) {
        @Nullable List<Label> targetsToSync = switch (params.selector()) {
            case WorkspaceBuildTargetSelector.AllTargets allTargets -> null;
            case WorkspaceBuildTargetSelector.SpecificTargets selector -> selector.targets();
        };
        return myProjectResolver.resolve(
            params.build(),
            targetsToSync,
            params.allTargets(),
            params.repoMapping(),
            params.taskId()
        );
    }

    @Override
    public CompletableFuture<PhasedSyncProject> workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams params) {
        return myFirstPhaseProjectResolver.resolve(params.taskId());
    }

    @Override
    public CompletableFuture<CompileResult> buildTargetCompile(CompileParams params) {
        return myExecuteService.compile(params);
    }

    @Override
    public CompletableFuture<AnalysisDebugResult> buildTargetAnalysisDebug(AnalysisDebugParams params) {
        return myExecuteService.analysisDebug(params);
    }

    @Override
    public CompletableFuture<TestResult> buildTargetTest(TestParams params) {
        return myExecuteService.test(params);
    }

    @Override
    public CompletableFuture<RunResult> buildTargetRun(RunParams params) {
        return myExecuteService.run(params);
    }

    @Override
    public CompletableFuture<WorkspaceDirectoriesResult> workspaceDirectories(RepoMapping repoMapping, TaskId taskId) {
        return myBspMapper.workspaceDirectories(repoMapping, taskId);
    }

    @Override
    public CompletableFuture<JvmToolchainInfo> jvmToolchainInfoForTarget(Label target) {
        return myBspMapper.jvmBuilderParamsForTarget(target);
    }

    @Override
    public <T> CompletableFuture<BazelQueryResult<T>> query(BazelQueryParams<T> params) {
        return BazelQuery.runBazelQuery(myBazelRunner, myProjectView, params);
    }
}
