package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import consulo.application.progress.ProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import io.grpc.stub.StreamObserver;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.AllRuleTargets;
import org.jetbrains.bazel.label.Label.SyntheticLabel;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.server.BazelTestFileNames;
import org.jetbrains.bazel.server.bep.BepEventHandler.BepEventHandlerContext;
import org.jetbrains.bazel.server.bep.BepEventHandler.BepEventHandlerProvider;
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService;
import org.jetbrains.bazel.server.sync.configuration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelper;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BspClientTestNotifier;
import org.jetbrains.bsp.protocol.BazelInvocationMetrics.AnalysisCacheInvalidation;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.CachedTestLog;
import org.jetbrains.bsp.protocol.CoverageReport;
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams;
import org.jetbrains.bsp.protocol.TaskFinishParams;
import org.jetbrains.bsp.protocol.TaskFinishParams.CompileReport;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskStartParams;
import org.jetbrains.bsp.protocol.TaskStartParams.CompileTask;
import org.jetbrains.bsp.protocol.TestStatus;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BepServer extends PublishBuildEventGrpc.PublishBuildEventImplBase {
    private final Project myProject;
    private final BazelTaskEventsHandler myTaskEventsHandler;
    private final DiagnosticsService myDiagnosticsService;
    private final TaskId myParentId;
    private final BazelPathsResolver myBazelPathsResolver;
    // consulo: RawProgressReporter is mapped to ProgressIndicator
    private final @Nullable ProgressIndicator myRawProgressReporter;

    private @Nullable TaskId myStartedEvent = null;
    private @Nullable BspClientTestNotifier myBspClientTestNotifier = null; // Present for test commands
    private final BepOutputBuilder myBepOutputBuilder;
    private final BuildProgressParser myBuildProgressParser = new BuildProgressParser();
    private final List<BepEventHandler> myCustomBepEventHandlers;

    public BepServer(
        Project project,
        BazelTaskEventsHandler taskEventsHandler,
        DiagnosticsService diagnosticsService,
        TaskId parentId,
        BazelPathsResolver bazelPathsResolver,
        @Nullable ProgressIndicator rawProgressReporter
    ) {
        myProject = project;
        myTaskEventsHandler = taskEventsHandler;
        myDiagnosticsService = diagnosticsService;
        myParentId = parentId;
        myBazelPathsResolver = bazelPathsResolver;
        myRawProgressReporter = rawProgressReporter;
        myBepOutputBuilder = new BepOutputBuilder(bazelPathsResolver);

        BepEventHandlerContext bepEventHandlerContext =
            new BepEventHandlerContext(project, parentId, taskEventsHandler, diagnosticsService, bazelPathsResolver);
        myCustomBepEventHandlers = BepEventHandlerProvider.getEpName()
            .getExtensionList()
            .stream()
            .map(it -> it.create(bepEventHandlerContext))
            .toList();

        myBepOutput = myBepOutputBuilder.build();
    }

    // consulo: upstream default value of rawProgressReporter
    public BepServer(
        Project project,
        BazelTaskEventsHandler taskEventsHandler,
        DiagnosticsService diagnosticsService,
        TaskId parentId,
        BazelPathsResolver bazelPathsResolver
    ) {
        this(project, taskEventsHandler, diagnosticsService, parentId, bazelPathsResolver, null);
    }

    @Override
    public void publishLifecycleEvent(PublishLifecycleEventRequest request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(
        StreamObserver<PublishBuildToolEventStreamResponse> responseObserver
    ) {
        return new BepStreamObserver(this, responseObserver);
    }

    public void handleEvent(BuildEvent buildEvent) {
        try {
            BuildEventStreamProtos.BuildEvent event = BuildEventStreamProtos.BuildEvent.parseFrom(buildEvent.getBazelEvent().getValue());

            LOGGER.trace("Got event " + event);
            handleBuildEventStreamProtosEvent(event);
        }
        catch (IOException e) {
            // consulo: error(String, String) is ambiguous in the Consulo Logger, the detail is appended to the message
            LOGGER.error("Error deserializing BEP proto: " + e);
        }
    }

    public void handleBuildEventStreamProtosEvent(BuildEventStreamProtos.BuildEvent event) {
        for (BepEventHandler customHandler : myCustomBepEventHandlers) {
            if (customHandler.handleEvent(event)) {
                return;
            }
        }
        processBuildStartedEvent(event);
        processOptions(event);
        processProgressEvent(event);
        processBuildMetrics(event);
        processFinishedEvent(event);
        processActionCompletedEvent(event);
        fetchNamedSet(event);
        processCompletedEvent(event);
        processAbortedEvent(event);
        processTestResult(event);
        processTestSummary(event);
        processConfiguration(event);
    }

    private void processConfiguration(BuildEventStreamProtos.BuildEvent event) {
        if (!event.hasConfiguration()) {
            return;
        }
        BuildEventStreamProtos.Configuration config = event.getConfiguration();
        WorkspaceConfiguration workspaceConfiguration = new WorkspaceConfiguration(
            WorkspaceConfigurationId.of(event.getId().getConfiguration().getId()),
            new WorkspaceConfigurationSummary(
                event.getId().getConfiguration().getId(),
                config.getMnemonic(),
                config.getPlatformName(),
                config.getCpu(),
                config.getIsTool()
            ),
            List.of()
        );
        myBepOutputBuilder.storeConfiguration(workspaceConfiguration);
    }

    private void processTestResult(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasTestResult()) {
            TaskId taskId = myStartedEvent;
            BspClientTestNotifier bspClientTestNotifier = this.myBspClientTestNotifier;
            if (taskId == null || bspClientTestNotifier == null) {
                return;
            }

            BuildEventStreamProtos.TestResult testResult = event.getTestResult();

            // TODO: this is the place where we could parse the test result and produce individual test events
            // TODO: there's some other interesting data
            //  If testing is requested, a TestResult event is sent for each test attempt,
            //  shard, and run per test. This allows BEP consumers to identify precisely
            //  which test actions failed their tests and identify the test outputs
            //  (such as logs, test.xml files) for each test action.

            TestStatus testStatus =
                switch (testResult.getStatus()) {
                    case NO_STATUS -> TestStatus.SKIPPED;
                    case PASSED -> TestStatus.PASSED;
                    case FLAKY -> TestStatus.FAILED;
                    case TIMEOUT -> TestStatus.FAILED;
                    case FAILED -> TestStatus.FAILED;
                    case INCOMPLETE -> TestStatus.SKIPPED;
                    case REMOTE_FAILURE -> TestStatus.IGNORED;
                    case FAILED_TO_BUILD -> TestStatus.CANCELLED;
                    case TOOL_HALTED_BEFORE_TESTING -> TestStatus.SKIPPED;
                    default -> TestStatus.FAILED;
                };

            Path coverageReport = findTestActionOutput(testResult, BazelTestFileNames.COVERAGE);
            if (coverageReport != null) {
                myTaskEventsHandler.onPublishCoverageReport(
                    new CoverageReport(
                        taskId,
                        coverageReport
                    )
                );
            }

            if (testResult.getCachedLocally()) {
                Path testLog = findTestActionOutput(testResult, BazelTestFileNames.LOG);

                if (testLog != null) {
                    myTaskEventsHandler.onCachedTestLog(
                        new CachedTestLog(
                            taskId,
                            testLog
                        )
                    );
                }
            }

            Path testXml = findTestActionOutput(testResult, BazelTestFileNames.XML);
            Label label = Label.parse(event.getId().getTestResult().getLabel());
            BuildTarget targetSummary = TargetStorage.getTargetStorage(myProject).getTargetSummary(label);
            TargetKind targetKind = targetSummary != null ? targetSummary.getKind() : null;
            // Test cases identified and sent to the client by TestXmlParser.
            int reportedSuites = testXml != null ? new TestXmlParser(bspClientTestNotifier).parseAndReport(taskId, testXml, targetKind) : 0;
            if (reportedSuites == 0) {
                // Nothing was reported per-test: the XML is missing, or present but empty / all-skipped.
                // Go tests via rules_go without verbose output don't emit <testcase> entries, so the parser
                // reports nothing. Fall back to a single target-level node so the target shows its status
                // instead of "No tests were found".
                String targetName = BazelRepoMappingUtils.toShortString(label, myProject);
                TaskId childId = taskId.uniqueSubTask("test");

                BazelTestDetails testDetails = BazelTestDetails.testSuite(targetName).build();
                bspClientTestNotifier.startAndFinishTest(testDetails, childId, testStatus, "Test finished", targetKind, null);
            }
        }
    }

    // consulo: Kotlin testActionOutputList.find { it.name == fileName.filename }?.let { bazelPathsResolver.resolve(it) }
    private @Nullable Path findTestActionOutput(BuildEventStreamProtos.TestResult testResult, BazelTestFileNames fileName) {
        return testResult.getTestActionOutputList()
            .stream()
            .filter(it -> it.getName().equals(fileName.getFilename()))
            .findFirst()
            .map(it -> myBazelPathsResolver.resolve(it))
            .orElse(null);
    }

    private void processTestSummary(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasTestSummary()) {
            // TODO: this is probably only relevant in remote scenarios
        }
    }

    private void fetchNamedSet(BuildEventStreamProtos.BuildEvent event) {
        if (event.getId().hasNamedSet()) {
            NamedSetOfFiles internedNamedSetOfFiles = intern(event.getNamedSetOfFiles());
            myBepOutputBuilder.storeNamedSet(
                event.getId().getNamedSet().getId(),
                internedNamedSetOfFiles
            );
        }
    }

    /**
     * Returns a copy of a {@link NamedSetOfFiles} with interned string references.
     * String references in {@link NamedSetOfFiles} are interned to conserve memory.
     * <p>
     * BEP protos often contain many duplicate strings both within a single stream and across
     * shards running in parallel, so string interner is used to share references.
     */

    private NamedSetOfFiles intern(NamedSetOfFiles receiver) {
        return receiver.toBuilder()
            .clearFiles()
            .addAllFiles(
                receiver.getFilesList().stream().map(file ->
                    file
                        .toBuilder()
                        .setUri(file.getUri().intern())
                        .setName(file.getName().intern())
                        .clearPathPrefix()
                        .addAllPathPrefix(
                            file.getPathPrefixList().stream().map(String::intern).toList()
                        ).build()
                ).toList()
            ).build();
    }

    private void processBuildStartedEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasStarted()) {
            consumeBuildStartedEvent(event);
        }
    }

    private void processOptions(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasOptionsParsed()) {
            myBepOutputBuilder.storeOptions(event.getOptionsParsed().getCmdLineList());
        }
    }

    private void processProgressEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasProgress()) {
            BuildEventStreamProtos.Progress progress = event.getProgress();
            if (!progress.getStderr().isEmpty()) {
                List<String> stderrLines = lines(progress.getStderr()).stream().map(AnsiEscapeCodes::strip).toList();

                List<PublishDiagnosticsParams> events =
                    myDiagnosticsService.extractDiagnostics(
                        stderrLines,
                        new SyntheticLabel(AllRuleTargets.INSTANCE),
                        myParentId,
                        true,
                        true
                    );
                events.forEach(it ->
                    myTaskEventsHandler.onBuildPublishDiagnostics(
                        it
                    )
                );

                BuildProgressParser.BuildProgress buildProgress = myBuildProgressParser.parse(stderrLines);
                if (buildProgress != null && myRawProgressReporter != null) {
                    myRawProgressReporter.setText2(LocalizeValue.ofNullable(buildProgress.details()));
                    // consulo: RawProgressReporter.fraction(null) makes the progress indeterminate
                    Double fraction = buildProgress.fraction();
                    if (fraction == null) {
                        myRawProgressReporter.setIndeterminate(true);
                    }
                    else {
                        myRawProgressReporter.setIndeterminate(false);
                        myRawProgressReporter.setFraction(fraction);
                    }
                }

                if (myAnalysisCacheInvalidation == null) {
                    myAnalysisCacheInvalidation = AnalysisCacheInvalidationParser.parse(stderrLines);
                }
            }

            // Also scan progress.stdout: with --curses=true (PTY mode), Bazel may route the warning there.
            if (myAnalysisCacheInvalidation == null && !progress.getStdout().isEmpty()) {
                List<String> stdoutLines = lines(progress.getStdout()).stream().map(AnsiEscapeCodes::strip).toList();
                myAnalysisCacheInvalidation = AnalysisCacheInvalidationParser.parse(stdoutLines);
            }
        }
    }

    private void processBuildMetrics(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasBuildMetrics()) {
            myBepMetrics = event.getBuildMetrics();
        }
    }

    private void consumeBuildStartedEvent(BuildEventStreamProtos.BuildEvent event) {
        myBepOutputBuilder.clear();

        BazelRelease buildToolVersion = BazelRelease.fromReleaseString(event.getStarted().getBuildToolVersion());
        myBepOutputBuilder.setBuildToolVersion(buildToolVersion != null ? buildToolVersion : BazelRelease.FALLBACK_VERSION);

        TaskId taskId = myParentId.subTask("started-" + event.getStarted().getUuid());
        TaskStartParams startParams = new TaskStartParams(taskId, event.getStarted().getStartTimeMillis(), null, null);
        @Nullable Label target = Label.parse(event.getId().getTestResult().getLabel());

        if (event.getStarted().getCommand().equals(Constants.BAZEL_BUILD_COMMAND)) { // todo: why only build?
            if (target != null) {
                CompileTask task = new CompileTask(target);
                startParams.setData(task);
            }
            myTaskEventsHandler.onBuildTaskStart(startParams);
        }
        else if (event.getStarted().getCommand().equals(Constants.BAZEL_TEST_COMMAND)
            || event.getStarted().getCommand().equals(Constants.BAZEL_COVERAGE_COMMAND)) {
            if (target == null) {
                return;
            }

            BspClientTestNotifier bspClientTestNotifier = new BspClientTestNotifier(myTaskEventsHandler);
            this.myBspClientTestNotifier = bspClientTestNotifier;
            bspClientTestNotifier.beginTestTarget(target, taskId);
        }

        myStartedEvent = taskId;
    }

    private void processFinishedEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasFinished()) {
            consumeFinishedEvent(event);
        }
    }

    private void consumeFinishedEvent(BuildEventStreamProtos.BuildEvent event) {
        TaskId taskId = myStartedEvent;
        @Nullable Label target = Label.parse(event.getId().getTestResult().getLabel());

        if (target == null) {
            LOGGER.warn("No target label was found. Task id: " + taskId);
            return;
        }

        if (taskId == null) {
            LOGGER.warn("No start event id was found. Origin id: " + myParentId);
            return;
        }

        if (myBspClientTestNotifier != null) {
            myBspClientTestNotifier = null;
            return;
        }

        BazelStatus statusCode = BazelStatus.fromExitCode(event.getFinished().getExitCode().getCode());
        boolean isSuccess = statusCode == BazelStatus.SUCCESS;
        int errors = isSuccess ? 0 : 1;
        CompileReport report = new CompileReport(target, errors, 0);
        TaskFinishParams finishParams =
            new TaskFinishParams(taskId, event.getFinished().getFinishTimeMillis(), null, statusCode, report);
        myTaskEventsHandler.onBuildTaskFinish(finishParams);
    }

    private void processCompletedEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasCompleted()) {
            consumeCompletedEvent(event);
        }
    }

    private void processActionCompletedEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasAction()) {
            consumeActionCompletedEvent(event.getAction());
        }
    }

    private void consumeActionCompletedEvent(BuildEventStreamProtos.ActionExecuted event) {
        Label label = Label.parse(event.getLabel());
        switch (event.getStderr().getFileCase()) {
            case URI -> {
                try {
                    Path path = myBazelPathsResolver.resolve(event.getStderr());
                    String stdErrText = Files.readString(path);
                    processDiagnosticText(stdErrText, label, myParentId);
                }
                catch (FileSystemNotFoundException e) {
                    LOGGER.warn("Failed to process diagnostic text", e);
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to process diagnostic text", e);
                }
            }

            case CONTENTS -> {
                processDiagnosticText(event.getStderr().getContents().toStringUtf8(), label, myParentId);
            }

            default -> {
            }
        }
    }

    private void processDiagnosticText(
        String stdErrText,
        Label targetLabel,
        TaskId taskId
    ) {
        if (!stdErrText.isEmpty()) {
            List<PublishDiagnosticsParams> events =
                myDiagnosticsService.extractDiagnostics(
                    lines(stdErrText),
                    targetLabel,
                    taskId
                );
            events.forEach(it ->
                myTaskEventsHandler.onBuildPublishDiagnostics(
                    it
                )
            );
        }
    }

    private void consumeCompletedEvent(BuildEventStreamProtos.BuildEvent event) {
        WorkspaceTargetKey eventTargetKey = WorkspaceHelper.getWorkspaceTargetKey(event.getId());
        if (eventTargetKey == null) {
            LOGGER.warn("No target key found in event " + event);
            return;
        }
        BuildEventStreamProtos.TargetComplete targetComplete = event.getCompleted();
        List<BuildEventStreamProtos.OutputGroup> outputGroups = targetComplete.getOutputGroupList();
        LOGGER.trace("Consuming target completed event " + targetComplete);

        // when configurations aren't supported, simply use `EMPTY` or fallback configuration IDs
        WorkspaceConfigurationId targetConfiguration;
        if (configuration.isConfigurationSupportEnabled(myBepOutputBuilder.getBuildToolVersion())) {
            targetConfiguration = eventTargetKey.configuration();
        }
        else {
            WorkspaceConfiguration workspaceConfiguration = myBepOutputBuilder.getConfigurations().get(eventTargetKey.configuration());
            if (workspaceConfiguration == null) {
                targetConfiguration = WorkspaceConfigurationId.EMPTY;
            }
            else if (WorkspaceHelper.isExecConfig(workspaceConfiguration)) {
                targetConfiguration = configuration.FALLBACK_EXEC_CONFIG;
            }
            else {
                targetConfiguration = configuration.FALLBACK_CONFIG;
            }
        }

        // match behavior of the aspect, remove intellij aspect entries
        WorkspaceAspectIds aspectIds = WorkspaceAspectIds.of(
            eventTargetKey.aspectIds().getIds()
                .stream()
                .filter(aspectId -> {
                    if (!aspectId.contains("%")) {
                        return true;
                    }
                    String name = aspectId.split("%", -1)[1];
                    return !StringUtil.trimStart(name, "_").startsWith("intellij");
                })
                .toList()
        );

        myBepOutputBuilder.storeTargetOutputGroups(
            new WorkspaceTargetKey(eventTargetKey.label(), targetConfiguration, aspectIds),
            outputGroups
        );
    }

    private void processAbortedEvent(BuildEventStreamProtos.BuildEvent event) {
        if (event.hasAborted()) {
            consumeAbortedEvent(event.getAborted());
        }
    }

    private void consumeAbortedEvent(BuildEventStreamProtos.Aborted aborted) {
        if (aborted.getReason() != BuildEventStreamProtos.Aborted.AbortReason.NO_BUILD) {
            LOGGER.warn("Command aborted with reason " + aborted.getReason() + ": " + aborted.getDescription());
        }
    }

    private final BepOutput myBepOutput;
    private BuildEventStreamProtos.@Nullable BuildMetrics myBepMetrics = null;

    public BepOutput getBepOutput() {
        return myBepOutput;
    }

    public BuildEventStreamProtos.@Nullable BuildMetrics getBepMetrics() {
        return myBepMetrics;
    }

    public BepOutput getOutputForTesting() {
        return myBepOutput;
    }

    /** Set by BEP progress event scanning; `null` if no discard was detected this invocation. */
    private @Nullable AnalysisCacheInvalidation myAnalysisCacheInvalidation = null;

    public @Nullable AnalysisCacheInvalidation getAnalysisCacheInvalidation() {
        return myAnalysisCacheInvalidation;
    }

    // consulo: Kotlin String.lines(), it keeps a trailing empty line unlike String.lines() of Java
    private static List<String> lines(String text) {
        return List.of(text.split("\r\n|\n|\r", -1));
    }

    private static final Logger LOGGER = Logger.getInstance(BepServer.class);
}
