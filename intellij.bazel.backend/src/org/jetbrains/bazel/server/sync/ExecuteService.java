package org.jetbrains.bazel.server.sync;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.project.Project;
import org.jetbrains.bazel.bazelrunner.BazelCommand;
import org.jetbrains.bazel.bazelrunner.BazelCommand.BazelCommandExecutionDescriptor;
import org.jetbrains.bazel.bazelrunner.BazelLog;
import org.jetbrains.bazel.bazelrunner.BazelProcess;
import org.jetbrains.bazel.bazelrunner.BazelProcessResult;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.bazelrunner.params.BazelFlag;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.TargetCollection;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.server.BazelTestFileNames;
import org.jetbrains.bazel.server.bep.AnalysisCacheInvalidationParser;
import org.jetbrains.bazel.server.bep.BazelInvocationMetricsMapper;
import org.jetbrains.bazel.server.bep.BepBuildResult;
import org.jetbrains.bazel.server.bep.BepOutput;
import org.jetbrains.bazel.server.bep.BepReader;
import org.jetbrains.bazel.server.bep.BepServer;
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics.AnalysisCacheInvalidation;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics.BazelInvocationContext;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.CompileParams;
import org.jetbrains.bsp.protocol.CompileResult;
import org.jetbrains.bsp.protocol.RunParams;
import org.jetbrains.bsp.protocol.RunResult;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugParams;
import org.jetbrains.bsp.protocol.RunWithDebugParams.AnalysisDebugResult;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TestParams;
import org.jetbrains.bsp.protocol.TestResult;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

public class ExecuteService {
    private final Project myProject;
    private final Path myWorkspaceRoot;
    private final BazelTaskEventsHandler myTaskEventsHandler;
    private final BazelRunner myBazelRunner;
    private final ProjectView myProjectView;
    private final BazelPathsResolver myBazelPathsResolver;
    private final RawProgressReporterProvider myRawProgressReporterProvider;

    // consulo: overload for the upstream default value of rawProgressReporterProvider
    public ExecuteService(
        Project project,
        Path workspaceRoot,
        BazelTaskEventsHandler taskEventsHandler,
        BazelRunner bazelRunner,
        ProjectView projectView,
        BazelPathsResolver bazelPathsResolver
    ) {
        this(project, workspaceRoot, taskEventsHandler, bazelRunner, projectView, bazelPathsResolver, RawProgressReporterProvider.current());
    }

    public ExecuteService(
        Project project,
        Path workspaceRoot,
        BazelTaskEventsHandler taskEventsHandler,
        BazelRunner bazelRunner,
        ProjectView projectView,
        BazelPathsResolver bazelPathsResolver,
        RawProgressReporterProvider rawProgressReporterProvider
    ) {
        myProject = project;
        myWorkspaceRoot = workspaceRoot;
        myTaskEventsHandler = taskEventsHandler;
        myBazelRunner = bazelRunner;
        myProjectView = projectView;
        myBazelPathsResolver = bazelPathsResolver;
        myRawProgressReporterProvider = rawProgressReporterProvider;
    }

    // consulo: overload for the upstream default value of pidDeferred
    private CompletableFuture<BepBuildResult> runWithBepServer(
        BazelCommand command,
        TaskId taskId,
        BazelInvocationContext context
    ) {
        return runWithBepServer(command, taskId, context, null);
    }

    private CompletableFuture<BepBuildResult> runWithBepServer(
        BazelCommand command,
        TaskId taskId,
        BazelInvocationContext context,
        @Nullable CompletableFuture<@Nullable Long> pidDeferred
    ) {
        return myRawProgressReporterProvider.invoke(rawProgressReporter -> {
            // consulo: upstream coroutineScope; the try/catch/finally of upstream runs when the future completes
            Path eventFile;
            try {
                eventFile =
                    Files.createTempFile("bazel-bep-output", null).toAbsolutePath();
            }
            catch (IOException e) {
                return CompletableFuture.failedFuture(new UncheckedIOException(e));
            }

            AtomicReference<@Nullable BepReader> bepReaderRef = new AtomicReference<>();
            CompletableFuture<BepBuildResult> result;
            try {
                command.useBes(eventFile);
                BazelCommandExecutionDescriptor executionDescriptor = command.buildExecutionDescriptor();
                DiagnosticsService diagnosticsService = new DiagnosticsService(myWorkspaceRoot);
                BepServer bepServer =
                    new BepServer(myProject, myTaskEventsHandler, diagnosticsService, taskId, myBazelPathsResolver, rawProgressReporter);
                BepReader bepReader = new BepReader(bepServer, eventFile);
                bepReaderRef.set(bepReader);
                // consulo: upstream async(Dispatchers.IO) { bepReader.start() }
                CompletableFuture<@Nullable Void> bepReaderDeferred = CompletableFuture
                    .supplyAsync(bepReader::start, Application.get().getInstance(ApplicationConcurrency.class).executor())
                    .thenCompose(started -> started.thenApply(ignored -> null));

                BazelProcess bazelProcess = myBazelRunner.runBazelCommand(
                    executionDescriptor,
                    taskId
                );
                if (pidDeferred != null) {
                    pidDeferred.complete(bazelProcess.getPid());
                }
                result = bazelProcess.waitAndGetResult().thenCompose(processResult -> {
                    bepReader.bazelBuildFinished();
                    return bepReaderDeferred.thenApply(ignored -> {
                        BuildEventStreamProtos.BuildMetrics metrics = bepServer.getBepMetrics();
                        if (metrics != null) {
                            BazelLog.write(writer -> {
                                writer.println("Metrics:");
                                writer.println(metrics.toString());
                            });
                        }

                        reportInvocationMetrics(context, bepServer, processResult);

                        BepOutput bepOutput = bepServer.getBepOutput();
                        return new BepBuildResult(processResult, bepOutput);
                    });
                });
            }
            catch (Throwable e) {
                result = CompletableFuture.failedFuture(e);
            }

            return result.whenComplete((value, error) -> {
                try {
                    if (error != null) {
                        BepReader bepReader = bepReaderRef.get();
                        // consulo: coroutineScope cancels the BEP reader when the block fails; here the reader is told to stop
                        if (bepReader != null) {
                            bepReader.bazelBuildFinished();
                        }
                        if (unwrap(error) instanceof CancellationException) {
                            if (BazelFeatureFlags.getKillServerOnCancel()) {
                                long serverPid = bepReader != null ? bepReader.getServerPid().get() : 0;
                                if (serverPid > 0) {
                                    // consulo: Consulo has no OSProcessUtil.killProcess(pid); ProcessHandle kills the process by its pid
                                    ProcessHandle.of(serverPid).ifPresent(ProcessHandle::destroyForcibly);
                                }
                            }
                        }
                    }
                }
                finally {
                    try {
                        Files.delete(eventFile);
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        });
    }

    /** Reports analysis-cache discards and action cache hit/miss stats for a finished invocation (FUS). */
    private void reportInvocationMetrics(
        BazelInvocationContext context,
        BepServer bepServer,
        BazelProcessResult processResult
    ) {
        AnalysisCacheInvalidation analysisCacheInvalidation = bepServer.getAnalysisCacheInvalidation();
        BuildEventStreamProtos.BuildMetrics bepMetrics = bepServer.getBepMetrics();
        myTaskEventsHandler.onBazelInvocationMetrics(
            new BazelInvocationMetrics(
                context,
                // BEP progress events are the primary source: Bazel routes its output through BEP when
                // --build_event_binary_path is set. Process streams are the fallback for output that arrives
                // before BEP starts, or in PTY mode where stderr is merged into stdout.
                analysisCacheInvalidation != null
                    ? analysisCacheInvalidation
                    : AnalysisCacheInvalidationParser.parse(processResult.getStderrLines(), processResult.getStdoutLines()),
                bepMetrics != null ? BazelInvocationMetricsMapper.toActionCacheStats(bepMetrics) : null,
                bepMetrics != null ? BazelInvocationMetricsMapper.analysisPhaseTimeMsOrNull(bepMetrics) : null
            )
        );
    }

    public CompletableFuture<CompileResult> compile(CompileParams params) {
        if (!params.targets().isEmpty()) {
            return build(params.targets(), params.taskId(), params.arguments())
                .thenApply(result -> new CompileResult(result.getBazelStatus()));
        }
        else {
            return CompletableFuture.completedFuture(new CompileResult(BazelStatus.FATAL_ERROR));
        }
    }

    public CompletableFuture<AnalysisDebugResult> analysisDebug(AnalysisDebugParams params) {
        CompletableFuture<BazelStatus> statusCode;
        if (!params.targets().isEmpty()) {
            List<String> debugFlags =
                List.of(BazelFlag.noBuild(), BazelFlag.starlarkDebug(), BazelFlag.starlarkDebugPort(params.port()));
            statusCode = build(params.targets(), params.taskId(), debugFlags).thenApply(BazelProcessResult::getBazelStatus);
        }
        else {
            statusCode = CompletableFuture.completedFuture(BazelStatus.FATAL_ERROR);
        }
        return statusCode.thenApply(AnalysisDebugResult::new);
    }

    public CompletableFuture<RunResult> run(RunParams params) {
        return runImpl(params, null, null);
    }

    private CompletableFuture<RunResult> runImpl(
        RunParams params,
        @Nullable List<String> additionalProgramArguments,
        @Nullable List<String> additionalOptions
    ) {
        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.run(params.target(), run -> {
                    run.getOptions().add(BazelFlag.color(true));
                    if (!params.checkVisibility()) {
                        run.getOptions().add(BazelFlag.checkVisibility(false));
                    }
                    if (additionalOptions != null) {
                        run.getOptions().addAll(additionalOptions);
                    }
                    if (additionalProgramArguments != null) {
                        run.getProgramArguments().addAll(additionalProgramArguments);
                    }
                    run.getEnvironment().putAll(params.environmentVariables());
                    run.getProgramArguments().addAll(params.arguments());
                    run.getAdditionalBazelOptions().addAll(params.additionalBazelParams());
                    run.setEnablePty(!params.captureStdout()); // otherwise stderr is merged into stdout
                })
            );
        return runWithBepServer(command, params.taskId(), BazelInvocationContext.RUN, params.pidDeferred()).thenApply(result ->
            new RunResult(
                params.taskId(),
                result.processResult().getBazelStatus(),
                params.captureStdout() ? new String(result.processResult().getStdout(), StandardCharsets.UTF_8) : null
            )
        );
    }

    public CompletableFuture<TestResult> test(TestParams params) {
        TargetCollection targetsSpec = new TargetCollection(params.targets(), List.of());
        BazelCommand command;
        if (params.useCoverage()) {
            command = myBazelRunner.buildBazelCommand(myProjectView, builder -> builder.coverage());
            command.getOptions().add(BazelFlag.combinedReportLcov());
        }
        else {
            command = myBazelRunner.buildBazelCommand(myProjectView, builder -> builder.test());
        }

        String instrumentationFilter = params.useCoverage() ? params.coverageInstrumentationFilter() : null;
        String testFilter = params.testFilter();
        // separate fields options should overwrite additional options
        List<String> separateFieldsOptions = new ArrayList<>();
        if (testFilter != null) {
            separateFieldsOptions.add(BazelFlag.testFilter(testFilter));
        }
        if (instrumentationFilter != null) {
            separateFieldsOptions.add(BazelFlag.instrumentationFilter(instrumentationFilter));
        }
        List<String> additionalOptions = new ArrayList<>(params.additionalBazelParams());
        additionalOptions.addAll(separateFieldsOptions);
        if (!additionalOptions.isEmpty()) {
            ((BazelCommand.HasAdditionalBazelOptions) command).getAdditionalBazelOptions().addAll(additionalOptions);
        }

        if (params.streamTestOutput()) {
            // Ensure streamed test output for live UI in IDE
            ensureTestOutputStreamed(command);
        }
        ((BazelCommand.HasEnvironment) command).getEnvironment().putAll(params.environmentVariables());
        ((BazelCommand.HasProgramArguments) command).getProgramArguments().addAll(params.arguments());
        command.getOptions().add(BazelFlag.buildEventBinaryPathConversion(false));
        // Ensure all test xml, log, and lcov files are downloaded even when remote_download_outputs=minimal is set
        for (BazelTestFileNames it : BazelTestFileNames.values()) {
            command.getOptions().add(BazelFlag.remoteDownloadRegex(it.getFilenameRegex()));
        }
        BazelCommand.HasMultipleTargets multipleTargetsCommand = (BazelCommand.HasMultipleTargets) command;
        multipleTargetsCommand.getTargets().addAll(targetsSpec.values());
        multipleTargetsCommand.getExcludedTargets().addAll(targetsSpec.excludedValues());

        // TODO: handle multiple targets
        return runWithBepServer(command, params.taskId(), BazelInvocationContext.TEST)
            .thenApply(result -> new TestResult(result.processResult().getBazelStatus()));
    }

    private CompletableFuture<BazelProcessResult> build(
        List<Label> allTargets,
        TaskId taskId,
        List<String> additionalArguments
    ) {
        // you must compute all targets before passing them to bound
        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.build(build -> {
                    build.getAdditionalBazelOptions().addAll(additionalArguments);
                    build.getTargets().addAll(allTargets);
                    build.setEnablePty(true);
                })
            );

        return runWithBepServer(command, taskId, BazelInvocationContext.BUILD).thenApply(BepBuildResult::processResult);
    }

    private void ensureTestOutputStreamed(BazelCommand command) {
        // Add to the beginning to make overriding possible
        command.getOptions().add(0, BazelFlag.testOutputStreamed());
    }

    // consulo: overload for the upstream default value of extraFlags
    public CompletableFuture<BepBuildResult> buildTargetsWithBep(
        TargetCollection targetsSpec,
        TaskId taskId
    ) {
        return buildTargetsWithBep(targetsSpec, List.of(), taskId);
    }

    public CompletableFuture<BepBuildResult> buildTargetsWithBep(
        TargetCollection targetsSpec,
        List<String> extraFlags,
        TaskId taskId
    ) {
        BazelCommand command =
            myBazelRunner.buildBazelCommand(myProjectView, builder ->
                builder.build(build -> {
                    build.getOptions().addAll(extraFlags);
                    build.getTargets().addAll(targetsSpec.values());
                    build.getExcludedTargets().addAll(targetsSpec.excludedValues());
                    build.setEnablePty(true);
                })
            );

        return runWithBepServer(command, taskId, BazelInvocationContext.SYNC);
    }

    // consulo: the failure of a CompletableFuture stage is wrapped into a CompletionException
    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}
