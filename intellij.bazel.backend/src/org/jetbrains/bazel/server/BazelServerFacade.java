package org.jetbrains.bazel.server;

import org.jetbrains.bazel.bazelrunner.ModuleResolver.ResolvedModulesAndWarning;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryParams;
import org.jetbrains.bazel.server.BazelQuery.BazelQueryResult;
import org.jetbrains.bazel.server.model.BazelSyncProject.AspectSyncProject;
import org.jetbrains.bazel.server.model.BazelSyncProject.PhasedSyncProject;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
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
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams;
import org.jetbrains.bsp.protocol.WorkspaceDirectories.WorkspaceDirectoriesResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BazelServerFacade {
    BazelInfo getBazelInfo();

    BazelOutFileHardLinks getOutFileHardLinks();

    BazelPathsResolver getBazelPathsResolver();

    ProjectView getProjectView();

    OutputLocationResolver getOutputResolver();

    OutputLocationParser getOutputParser();

    CompletableFuture<RepoMapping> workspaceRepoMapping(TaskId taskId);

    CompletableFuture<ResolvedModulesAndWarning> showRepos(List<String> repoNames, TaskId taskId);

    CompletableFuture<AspectSyncProject> workspaceBuildTargets(WorkspaceBuildTargetParams params);

    CompletableFuture<PhasedSyncProject> workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams params);

    CompletableFuture<CompileResult> buildTargetCompile(CompileParams params);

    /**
     * {@link #buildTargetRun} can also be used for debugging if you pass params into
     * {@link org.jetbrains.bsp.protocol.RunParams#additionalBazelParams()}
     */
    CompletableFuture<RunResult> buildTargetRun(RunParams params);

    CompletableFuture<TestResult> buildTargetTest(TestParams params);

    CompletableFuture<WorkspaceDirectoriesResult> workspaceDirectories(RepoMapping repoMapping, TaskId taskId);

    CompletableFuture<AnalysisDebugResult> buildTargetAnalysisDebug(AnalysisDebugParams params);

    CompletableFuture<JvmToolchainInfo> jvmToolchainInfoForTarget(Label target);

    <T> CompletableFuture<BazelQueryResult<T>> query(BazelQueryParams<T> params);
}
