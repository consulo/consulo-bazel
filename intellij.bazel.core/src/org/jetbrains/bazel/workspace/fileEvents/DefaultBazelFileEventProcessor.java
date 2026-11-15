package org.jetbrains.bazel.workspace.fileEvents;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.content.ContentFolderTypeProvider;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkSrcsListEval;
import org.jetbrains.bazel.progress.ShowConsole;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.run.task.BazelBuildTaskListener;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.sync.FileToTargetQuery;
import org.jetbrains.bazel.sync.ProjectDirtyStateService;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.environment.BazelProjectInfoService;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bazel.ui.status.BazelFileStatusRefresher;
import org.jetbrains.bazel.workspace.BazelProjectDirectoriesUtils;
import org.jetbrains.bazel.workspace.indexing.IndexableContentCollector;
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

// consulo: upstream declares it in BazelFileEventProcessor.kt; @ServiceImpl classes need their own file.
// The IntelliJ workspace model is not used: the "entity storage diff" is a ModelDiff of classic ModifiableRootModels (content and
// source roots of the Bazel modules) plus the BazelProjectDirectoriesEntity project data, applied in one write action.
// The coroutine Channel loop is a single-threaded executor that drains the queue after the same processing delay.
@ServiceImpl
@Singleton
public class DefaultBazelFileEventProcessor implements BazelFileEventProcessor {
    private static final Logger LOG = Logger.getInstance(BazelFileEventProcessor.class);

    private static final long PROCESSING_DELAY = 250; // not noticeable by the user, but if there are many events simultaneously, we will get them all

    private final Project myProject;
    private final TargetStorage myTargetUtils;
    private final Queue<EventsBatch> myEventsQueue = new ConcurrentLinkedQueue<>();

    private final AtomicInteger myEventsRequestCounter = new AtomicInteger(0);
    private final AtomicInteger myEventsProcessCounter = new AtomicInteger(0);

    private final ExecutorService myEventsExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Bazel file events", 1);
    private final AtomicBoolean myDraining = new AtomicBoolean(false);

    private record EventsBatch(
        List<SimplifiedFileEvent> events,
        CompletableFuture<BazelFileEventProcessorResult> result
    ) {
    }

    @Inject
    public DefaultBazelFileEventProcessor(Project project) {
        myProject = project;
        myTargetUtils = TargetStorage.getTargetStorage(project);
    }

    // consulo: upstream init { BazelCoroutineService.start { while (true) { receive; delay; tryReceive...; processEventsBatch } } }
    private void scheduleDrain() {
        if (myDraining.compareAndSet(false, true)) {
            myEventsExecutor.execute(this::drainQueue);
        }
    }

    private void drainQueue() {
        while (true) {
            EventsBatch first = myEventsQueue.poll();
            if (first == null) {
                myDraining.set(false);
                // an event may have been queued between poll() and set(false)
                if (myEventsQueue.isEmpty() || !myDraining.compareAndSet(false, true)) {
                    return;
                }
                continue;
            }
            List<EventsBatch> batches = new ArrayList<>();
            batches.add(first);
            // load all available events after small pause
            EventsBatch moreEvents;
            do {
                try {
                    Thread.sleep(PROCESSING_DELAY);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                moreEvents = myEventsQueue.poll();
                if (moreEvents != null) {
                    batches.add(moreEvents);
                }
            }
            while (moreEvents != null);
            try {
                processEventsBatch(batches);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public CompletableFuture<BazelFileEventProcessorResult> enqueue(List<VFileEvent> events) {
        // if a project has no targets, there is no point in processing (also, it could interrupt the initial sync)
        return myTargetUtils.awaitLoaded().thenCompose(unused -> {
            if (myTargetUtils.getTotalTargetCount() == 0) {
                return CompletableFuture.completedFuture(BazelFileEventProcessorResult.EMPTY);
            }

            // No directory is known. Startup or tests?
            if (BazelProjectInfoService.getBazelProjectInfo(myProject).getProjectRootDir() == null) {
                return CompletableFuture.completedFuture(BazelFileEventProcessorResult.EMPTY);
            }

            List<SimplifiedFileEvent> simplifiedEvents = new ArrayList<>();
            for (VFileEvent event : events) {
                SimplifiedFileEvent simplified = SimplifiedFileEvent.from(event);
                if (simplified != null && simplified.shouldBeProcessed(myProject)) {
                    simplifiedEvents.add(simplified);
                }
            }
            List<SimplifiedFileEvent> filteredEvents = filterByProject(simplifiedEvents);

            if (filteredEvents.isEmpty()) {
                return CompletableFuture.completedFuture(BazelFileEventProcessorResult.EMPTY);
            }

            CompletableFuture<BazelFileEventProcessorResult> result = new CompletableFuture<>();
            myEventsRequestCounter.addAndGet(filteredEvents.size());
            myEventsQueue.add(new EventsBatch(filteredEvents, result));
            scheduleDrain();
            return result;
        });
    }

    @Override
    public boolean isIdle() {
        return myEventsRequestCounter.get() == myEventsProcessCounter.get();
    }

    private boolean getAllowBazelQuery() {
        return BazelFeatureFlags.getQueryBazelOnFileEvents();
    }

    private void processEventsBatch(List<EventsBatch> batches) {
        FileEventJobManager jobManager = FileEventJobManager.getInstance(myProject);
        TaskId lastSyncTaskId = myProject.getInstance(ProjectSyncService.class).getLastSyncTaskId();
        TaskId.TaskGroupId lastSyncTaskGroupId = lastSyncTaskId == null ? null : lastSyncTaskId.taskGroupId();
        TaskId.TaskGroupId taskGroupId = lastSyncTaskGroupId != null ? lastSyncTaskGroupId : new TaskId.TaskGroupId("file-event-" + randomHex(8));
        TaskId taskId = taskGroupId.task("file-event-processing");

        List<SimplifiedFileEvent> events = new ArrayList<>();
        for (EventsBatch batch : batches) {
            events.addAll(batch.events());
        }
        AtomicReference<BazelFileEventProcessorResult> result = new AtomicReference<>(BazelFileEventProcessorResult.EMPTY);

        @Nullable CompletableFuture<Void> processingJob = jobManager.runFileEventsProcessing(() ->
            BazelCoroutineService.getInstance(myProject).startAsync(true, () -> {
                ModelDiff entityStorageDiff = new ModelDiff();
                return BazelFileEventProgressReporter.runWithProgressBar(myProject, bazelReporter -> {
                    ProcessingContext context = new ProcessingContext(
                        entityStorageDiff,
                        taskId,
                        bazelReporter
                    );

                    return processEventsBatchImpl(
                        events,
                        context
                    ).thenApply(processed -> {
                        result.set(processed);
                        return null;
                    });
                }).whenComplete((value, error) -> {
                    if (error != null && !(unwrap(error) instanceof CancellationException)) {
                        LOG.error(error);
                    }
                    if (error != null) {
                        entityStorageDiff.dispose();
                    }
                });
            })
        );

        try {
            if (processingJob == null) {
                // Cannot spawn job, most likely sync is in progress
                return;
            }

            // no need to start sync console task if no Bazel processing is allowed
            if (getAllowBazelQuery()) {
                startSyncConsoleTask(myProject, processingJob, taskId);
            }
            try {
                processingJob.join();
            }
            catch (CancellationException | CompletionException ignored) {
                // the failure is reported by the job itself (logged, or shown in the sync console)
            }
            finally {
                BazelTaskEventsService.getInstance(myProject).removeListener(taskId.taskGroupId());
            }
        }
        finally {
            myEventsProcessCounter.addAndGet(events.size());
            for (EventsBatch batch : batches) {
                try {
                    batch.result().complete(result.get());
                }
                catch (Throwable e) {
                    LOG.warn(e);
                }
            }
            BazelFileStatusRefresher.getInstance(myProject).refreshAllFilesPresentation();
        }
    }

    private void startSyncConsoleTask(Project project, CompletableFuture<?> processingJob, TaskId taskId) {
        TaskConsole syncConsole = util.getSyncConsole(project);
        syncConsole.startTask(
            taskId,
            BazelPluginLocalize.fileChangeProcessingTitle(),
            BazelPluginLocalize.fileChangeProcessingMessageStart(),
            () -> processingJob.cancel(true),
            null,
            ShowConsole.ON_FAIL
        );
        BazelBuildTaskListener taskListener = new BazelBuildTaskListener(syncConsole);
        BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId(), taskListener);
        processingJob.whenComplete((value, error) -> {
            BuildEventFactory buildEventFactory = Application.get().getInstance(BuildEventFactory.class);
            Throwable exception = error == null ? null : unwrap(error);
            LocalizeValue message;
            EventResult eventResult;
            if (exception == null) {
                message = BazelPluginLocalize.fileChangeProcessingMessageFinish();
                eventResult = buildEventFactory.createSuccessResult();
            }
            else if (exception instanceof CancellationException) {
                message = BazelPluginLocalize.fileChangeProcessingMessageCancelled();
                eventResult = buildEventFactory.createSkippedResult();
            }
            else {
                message = BazelPluginLocalize.fileChangeProcessingMessageFailed();
                eventResult = buildEventFactory.createFailureResult(exception);
            }
            util.getSyncConsole(project).finishTask(taskId, message, eventResult);
        });
    }

    private CompletableFuture<BazelFileEventProcessorResult> processEventsBatchImpl(
        List<SimplifiedFileEvent> events,
        ProcessingContext context
    ) {
        List<SimplifiedFileEvent> planarizedEvents = new ArrayList<>();
        for (SimplifiedFileEvent event : events) {
            VirtualFile newVirtualFile = event.getNewVirtualFile();
            if (event instanceof SimplifiedFileEvent.CreateDirectory) {
                planarizedEvents.addAll(collectCreatedFiles(newVirtualFile));
            }
            else if (newVirtualFile != null && newVirtualFile.isDirectory()) {
                // ignore other directory events
            }
            else {
                planarizedEvents.add(event);
            }
        }

        List<SimplifiedFileEvent> sourceEvents = planarizedEvents.stream().filter(it -> it.affectsSourceFile(myProject)).toList();
        return doProcessSourceFileEvents(sourceEvents, context).thenCompose(sourceProcessingResult -> {
            boolean sourceModelChanged = !sourceProcessingResult.getRemovedFromModel().isEmpty() ||
                !sourceProcessingResult.getAddedToModel().isEmpty();

            doProcessBazelFileEvents(planarizedEvents.stream().filter(SimplifiedFileEvent::affectsBazelConfigFile).toList());

            boolean indexedFilesModelChanged = ReadAction.compute(() -> updateIndexedFilesInModel(planarizedEvents, context));

            // Finalize and apply changes
            if (sourceModelChanged || indexedFilesModelChanged) {
                return context.progressReporter().finalisingStep(() ->
                    // consulo: upstream workspaceModel.update("File event processing (Bazel)") { applyChangesFrom(diff) }
                    myProject.getUIAccess().giveAsync(() -> {
                        WriteAction.run(() -> context.entityStorageDiff().commit());
                    })
                ).thenApply(unused -> sourceProcessingResult);
            }

            context.entityStorageDiff().dispose();
            return CompletableFuture.completedFuture(sourceProcessingResult);
        });
    }

    private List<SimplifiedFileEvent.Create> collectCreatedFiles(@Nullable VirtualFile root) {
        if (root == null || !root.isValid()) {
            return List.of();
        }

        List<SimplifiedFileEvent.Create> filesInDirectory = new ArrayList<>();
        try {
            // consulo: iterateContentUnderDirectory needs a read action in Consulo
            ReadAction.run(() -> ProjectFileIndex.getInstance(myProject).iterateContentUnderDirectory(root, file -> {
                if (!file.isValid()) {
                    return true;
                }
                if (!file.isDirectory()) {
                    SimplifiedFileEvent.Create createFileEvent = new SimplifiedFileEvent.Create(file.getPath(), file);
                    if (createFileEvent.shouldBeProcessed(myProject)) {
                        filesInDirectory.add(createFileEvent);
                    }
                }
                return true;
            }));
        }
        catch (IllegalArgumentException ex) {
            LOG.debug("Stopped collecting created files for stale directory " + root.getPath(), ex);
        }
        return filesInDirectory;
    }

    @RequiredReadAction
    private boolean updateIndexedFilesInModel(
        List<SimplifiedFileEvent> events,
        ProcessingContext context
    ) {
        BazelProjectDirectoriesEntity directoriesEntity = context.entityStorageDiff().getBazelProjectDirectoriesEntity();
        if (directoriesEntity == null) {
            return false;
        }
        Set<VirtualFile> indexableRecursiveRoots = BazelProjectDirectoriesUtils.toVirtualFileSet(directoriesEntity.indexableRecursiveRoots());
        Supplier<IndexableContentCollector> indexableContentCollector = LazyValue.notNull(() ->
            new IndexableContentCollector(
                myProject,
                directoriesEntity.indexPatterns(),
                BazelProjectDirectoriesUtils.toVirtualFileSet(directoriesEntity.includedRoots()),
                BazelProjectDirectoriesUtils.toVirtualFileSet(directoriesEntity.excludedRoots()),
                context.entityStorageDiff().getContentRootFiles()
            )
        );
        Set<String> removedUrls = new HashSet<>();
        Set<String> newUrls = new HashSet<>();
        for (SimplifiedFileEvent it : events) {
            Path fileRemoved = it.getFileRemoved();
            if (fileRemoved != null) {
                removedUrls.add(VirtualFileUtil.pathToUrl(fileRemoved.toString()));
            }
            VirtualFile newVirtualFile = it.getNewVirtualFile();
            if (newVirtualFile == null) {
                continue;
            }
            // already indexed under recursive roots
            if (VirtualFileUtil.isUnder(newVirtualFile, indexableRecursiveRoots)) {
                continue;
            }
            // not specified for indexing
            if (!indexableContentCollector.get().shouldBeIndexed(newVirtualFile)) {
                continue;
            }
            newUrls.add(newVirtualFile.getUrl());
        }
        if (removedUrls.isEmpty() && newUrls.isEmpty()) {
            return false;
        }
        Set<String> updatedIndexableNonRecursiveRoots = new HashSet<>();
        for (String url : directoriesEntity.indexableNonRecursiveRoots()) {
            if (!removedUrls.contains(url)) {
                updatedIndexableNonRecursiveRoots.add(url);
            }
        }
        updatedIndexableNonRecursiveRoots.addAll(newUrls);
        if (updatedIndexableNonRecursiveRoots.equals(new HashSet<>(directoriesEntity.indexableNonRecursiveRoots()))) {
            return false;
        }
        context.entityStorageDiff().setBazelProjectDirectoriesEntity(
            new BazelProjectDirectoriesEntity(
                directoriesEntity.projectRoot(),
                directoriesEntity.includedRoots(),
                directoriesEntity.excludedRoots(),
                directoriesEntity.indexPatterns(),
                directoriesEntity.indexableRecursiveRoots(),
                new ArrayList<>(updatedIndexableNonRecursiveRoots)
            )
        );
        return true;
    }

    private CompletableFuture<BazelFileEventProcessorResult> doProcessSourceFileEvents(
        List<SimplifiedFileEvent> events,
        ProcessingContext context
    ) {
        if (events.isEmpty()) {
            return CompletableFuture.completedFuture(BazelFileEventProcessorResult.EMPTY);
        }
        ArrayDeque<Path> failedEvalPaths = new ArrayDeque<>();
        return context.progressReporter().queryStep(() -> {
            Map<Path, PathAndVFile> files = new LinkedHashMap<>();
            for (SimplifiedFileEvent event : events) {
                PathAndVFile file = toPathAndVFile(event);
                if (file != null && !file.vFile().isDirectory()) {
                    files.putIfAbsent(file.path(), file);
                }
            }
            // avoid running a Bazel query when not required (BAZEL-2458)
            if (!files.isEmpty()) {
                return invertedSourcesQuery(context.taskId(), files.values(), failedEvalPaths, context);
            }
            else {
                return CompletableFuture.completedFuture(Map.<Path, List<Label>>of());
            }
        }).thenCompose(targetsByPath -> {
            Set<Path> visitedRemovedPaths = new HashSet<>();
            Set<Path> visitedAddedPaths = new HashSet<>();

            Set<Path> removedFromWSM = new HashSet<>();
            Set<Path> addedToWSM = new HashSet<>();

            return context.progressReporter().updateModelStep(() -> {
                ReadAction.run(() -> {
                    for (SimplifiedFileEvent event : events) {
                        Path removedFile = event.getFileRemoved();
                        Path addedFile = event.getFileAdded();

                        context.progressReporter().message(
                            addedFile != null ? addedFile.toString() : removedFile != null ? removedFile.toString() : ""
                        );

                        String removedFileUrl = removedFile == null ? null : VirtualFileUtil.pathToUrl(removedFile.toString());
                        String addedFileUrl = addedFile == null ? null : VirtualFileUtil.pathToUrl(addedFile.toString());

                        Set<Label> oldTargets = removedFile == null ? Set.of() : new HashSet<>(myTargetUtils.getTargetsForPath(removedFile));
                        List<Label> newTargetList = addedFile == null ? null : targetsByPath.get(addedFile);
                        Set<Label> newTargets = newTargetList == null ? Set.of() : new HashSet<>(newTargetList);

                        if (removedFile != null && !oldTargets.isEmpty() && visitedRemovedPaths.add(removedFile)) {
                            myTargetUtils.removeFileToTargetIdEntry(removedFile);
                            for (Label toRemove : oldTargets) {
                                for (Module module : toModuleEntities(toRemove, myProject)) {
                                    ModifiableRootModel rootModel = context.entityStorageDiff().getRootModel(module);
                                    List<ContentEntry> contentRoots = new ArrayList<>();
                                    for (ContentEntry it : rootModel.getContentEntries()) {
                                        if (it.getUrl().equals(removedFileUrl) ||
                                            !newTargets.contains(toRemove) && it.getUrl().equals(addedFileUrl)) {
                                            contentRoots.add(it);
                                        }
                                    }
                                    if (!contentRoots.isEmpty()) {
                                        contentRoots.forEach(rootModel::removeContentEntry);
                                        removedFromWSM.add(removedFile);
                                    }
                                }
                            }
                        }

                        if (addedFile != null && !newTargets.isEmpty() && visitedAddedPaths.add(addedFile) && addedFileUrl != null) {
                            myTargetUtils.addFileToTargetIdEntry(addedFile, newTargets);
                            for (Label toAdd : newTargets) {
                                for (Module module : toModuleEntities(toAdd, myProject)) {
                                    if (addFileToModule(addedFileUrl, context.entityStorageDiff(), module)) {
                                        addedToWSM.add(addedFile);
                                    }
                                }
                            }
                        }
                    }
                });
                return CompletableFuture.completedFuture(
                    new BazelFileEventProcessorResult(
                        removedFromWSM,
                        addedToWSM,
                        failedEvalPaths
                    )
                );
            });
        });
    }

    /**
     * Marks the package of every changed Bazel configuration file dirty, so that the IDE offers a sync.
     */
    private void doProcessBazelFileEvents(List<SimplifiedFileEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        Set<Path> paths = new LinkedHashSet<>();
        for (SimplifiedFileEvent event : events) {
            paths.addAll(event.affectedPaths());
        }
        List<String> buildFileNames = Arrays.asList(Constants.BUILD_FILE_NAMES);
        if (paths.stream().anyMatch(it -> !buildFileNames.contains(it.getFileName().toString()))) {
            ProjectDirtyStateService.getInstance(myProject).markWholeProjectDirty("Config file change");
            return;
        }

        List<Path> parents = new ArrayList<>();
        for (Path path : paths) {
            parents.add(path.getParent());
        }
        ProjectDirtyStateService.getInstance(myProject).markDirty(parents);
    }

    protected CompletableFuture<Map<Path, List<Label>>> invertedSourcesQuery(
        TaskId taskId,
        Collection<PathAndVFile> files,
        Collection<Path> failedEvalPaths,
        ProcessingContext context
    ) {
        // Heuristically evaluate targets by looking into PSI
        List<PathAndVFile> missingEvaluation = new ArrayList<>();
        StarlarkSrcsListEval eval = new StarlarkSrcsListEval(myProject);

        Map<Path, List<Label>> evaluated = new LinkedHashMap<>();
        for (PathAndVFile file : files) {
            context.progressReporter().message(file.path().toString());
            Map<Label, EnumSet<StarlarkSrcsListEval.Kind>> targets = ReadAction.compute(() -> eval.findTargetsForSourceFile(file.vFile()));
            if (targets.isEmpty()) {
                // Cannot determine the targets for file - show "Resync" button.
                missingEvaluation.add(file);
                continue;
            }
            List<Label> srcTargets = new ArrayList<>();
            for (Map.Entry<Label, EnumSet<StarlarkSrcsListEval.Kind>> entry : targets.entrySet()) {
                if (entry.getValue().contains(StarlarkSrcsListEval.Kind.Srcs)) {
                    srcTargets.add(entry.getKey());
                }
            }
            if (srcTargets.isEmpty()) {
                // file is in resources only. Everything OK, skip it
                continue;
            }

            evaluated.put(file.path(), srcTargets);
        }

        CompletableFuture<Map<Path, List<Label>>> queriedFuture;
        if (!missingEvaluation.isEmpty() && getAllowBazelQuery()) {
            LOG.warn("Cannot evaluate targets for " + missingEvaluation.size() + " new files. Querying Bazel: " +
                joinFirst3(missingEvaluation.stream().map(it -> it.toString()).toList()));
            context.progressReporter().message(BazelPluginLocalize.fileChangeProcessingStepQuery().get());
            queriedFuture = queryTargetsForFile(myProject, missingEvaluation.stream().map(PathAndVFile::path).toList(), taskId)
                .thenApply(it -> it != null ? it : Map.of());
        }
        else {
            queriedFuture = CompletableFuture.completedFuture(Map.of());
        }

        return queriedFuture.thenApply(queried -> {
            List<PathAndVFile> missingFiles = missingEvaluation.stream().filter(it -> !queried.containsKey(it.path())).toList();
            if (!missingFiles.isEmpty()) {
                LOG.warn(
                    "Cannot evaluate targets for " + missingFiles.size() + " new files. Show \"Sync Bazel changes\" button. " +
                        joinFirst3(missingFiles.stream().map(it -> it.path().toString()).toList())
                );
                failedEvalPaths.addAll(missingFiles.stream().map(PathAndVFile::path).toList());

                // Marks the BUILD file that owns each file of [missingFiles] as dirty.
                ProjectDirtyStateService dirtyState = ProjectDirtyStateService.getInstance(myProject);
                for (PathAndVFile file : missingFiles) {
                    VirtualFile buildFile = ReadAction.compute(() -> eval.findBuildFileForSourceFile(file.vFile()));
                    if (buildFile != null && buildFile.getParent() != null) {
                        dirtyState.markDirty(List.of(buildFile.getParent().toNioPath()));
                    }
                }
            }

            Map<Path, List<Label>> merged = new LinkedHashMap<>(evaluated);
            merged.putAll(queried);
            return merged;
        });
    }

    // consulo: Kotlin joinToString() of take(3)
    private static String joinFirst3(List<String> values) {
        return String.join(", ", values.subList(0, Math.min(3, values.size())));
    }

    protected record PathAndVFile(Path path, VirtualFile vFile) {
    }

    private static @Nullable PathAndVFile toPathAndVFile(SimplifiedFileEvent receiver) {
        Path fileAdded = receiver.getFileAdded();
        VirtualFile newVirtualFile = receiver.getNewVirtualFile();
        if (fileAdded != null && newVirtualFile != null) {
            return new PathAndVFile(fileAdded, newVirtualFile);
        }
        else {
            return null;
        }
    }

    // return true if was added, false if already present in content roots
    @RequiredReadAction
    private boolean addFileToModule(
        String url,
        ModelDiff entityStorageDiff,
        Module module
    ) {
        ModifiableRootModel rootModel = entityStorageDiff.getRootModel(module);
        // we don't want to duplicate source content roots
        if (hasSourceRootFor(rootModel, url)) {
            return false;
        }

        extensions.BazelModuleExtensionEntity bazelModuleExtension = extensions.getBazelModuleExtension(module);
        String sourceRootTypeId = bazelModuleExtension == null ? null : bazelModuleExtension.rootTypeId().defaultId();
        if (sourceRootTypeId == null) {
            return false;
        }
        // consulo: the source root type id is the id of a ContentFolderTypeProvider
        ContentFolderTypeProvider sourceRootType = ContentFolderTypeProvider.byId(sourceRootTypeId);
        if (sourceRootType == null) {
            return false;
        }

        ContentEntry contentRoot = rootModel.addContentEntry(url);
        contentRoot.addFolder(url, sourceRootType);
        return true;
    }

    private static boolean hasSourceRootFor(ModifiableRootModel rootModel, String url) {
        for (ContentEntry contentRoot : rootModel.getContentEntries()) {
            for (String sourceRootUrl : contentRoot.getFolderUrls(ContentFolderTypeProvider.allExceptExcluded())) {
                if (VirtualFileUtil.isEqualOrAncestor(sourceRootUrl, url)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<SimplifiedFileEvent> filterByProject(List<SimplifiedFileEvent> receiver) {
        if (receiver.isEmpty() || !org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            return List.of();
        }
        Path rootDirPath = projectRootPath();
        if (rootDirPath == null) {
            return List.of();
        }
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        return ReadAction.compute(() ->
            receiver.stream().filter(it -> it.doesAffectFolder(rootDirPath) && !it.affectsExcludedFiles(fileIndex, fileManager)).toList()
        );
    }

    private @Nullable Path projectRootPath() {
        try {
            return org.jetbrains.bazel.config.project.getRootDir(myProject).toNioPath();
        }
        catch (IllegalStateException ignored) { // Bazel rootDir not set
            return null;
        }
        catch (UnsupportedOperationException ignored) { // unable to create a Path instance
            return null;
        }
    }

    protected record ProcessingContext(
        ModelDiff entityStorageDiff,
        TaskId taskId,
        BazelFileEventProgressReporter progressReporter
    ) {
    }

    /**
     * consulo: replaces upstream's {@code MutableEntityStorage.from(workspaceModel.currentSnapshot)}: the root models of the modules
     * changed by the processing and the updated BazelProjectDirectoriesEntity, applied by {@link #commit()}.
     */
    protected final class ModelDiff {
        private final Map<Module, ModifiableRootModel> myRootModels = new LinkedHashMap<>();
        private @Nullable BazelProjectDirectoriesEntity myProjectDirectoriesEntity =
            BazelProjectModelService.getInstance(myProject).getProjectData(BazelProjectDirectoriesEntity.class);
        private boolean myProjectDirectoriesEntityChanged;

        @RequiredReadAction
        ModifiableRootModel getRootModel(Module module) {
            return myRootModels.computeIfAbsent(module, it -> ModuleRootManager.getInstance(it).getModifiableModel());
        }

        @Nullable BazelProjectDirectoriesEntity getBazelProjectDirectoriesEntity() {
            return myProjectDirectoriesEntity;
        }

        void setBazelProjectDirectoriesEntity(BazelProjectDirectoriesEntity entity) {
            myProjectDirectoriesEntity = entity;
            myProjectDirectoriesEntityChanged = true;
        }

        /**
         * Content roots of all modules, as {@code entities<ContentRootEntity>()} of the diff storage
         */
        @RequiredReadAction
        Set<VirtualFile> getContentRootFiles() {
            Set<VirtualFile> result = new HashSet<>();
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                ModifiableRootModel changed = myRootModels.get(module);
                for (ContentEntry entry : changed != null ? changed.getContentEntries() : ModuleRootManager.getInstance(module).getContentEntries()) {
                    VirtualFile file = entry.getFile();
                    if (file != null) {
                        result.add(file);
                    }
                }
            }
            return result;
        }

        void commit() {
            List<ModifiableRootModel> models = new ArrayList<>();
            for (ModifiableRootModel model : myRootModels.values()) {
                if (model.isChanged()) {
                    models.add(model);
                }
                else {
                    model.dispose();
                }
            }
            myRootModels.clear();
            if (!models.isEmpty()) {
                ModifiableModelCommitter.getInstance(myProject).multiCommit(
                    models.toArray(ModifiableRootModel[]::new),
                    ModuleManager.getInstance(myProject).getModifiableModel()
                );
            }
            BazelProjectDirectoriesEntity projectDirectoriesEntity = myProjectDirectoriesEntity;
            if (myProjectDirectoriesEntityChanged && projectDirectoriesEntity != null) {
                BazelProjectModelService.getInstance(myProject).putProjectData(BazelProjectDirectoriesEntity.class, projectDirectoriesEntity);
            }
        }

        void dispose() {
            for (ModifiableRootModel model : myRootModels.values()) {
                model.dispose();
            }
            myRootModels.clear();
        }
    }

    private static List<Module> toModuleEntities(Label receiver, Project project) {
        return project.getInstance(ModuleTargetService.class).findModulesByLabel(receiver);
    }

    private static CompletableFuture<@Nullable Map<Path, List<Label>>> queryTargetsForFile(
        Project project,
        List<Path> filePaths,
        TaskId taskId
    ) {
        if (SyncStatusService.getInstance(project).isSyncInProgress()) {
            return CompletableFuture.completedFuture(null);
        }

        return project.getInstance(WorkspaceSnapshotService.class).currentSnapshot()
            .thenCompose(snapshot -> BazelServerConnection.getConnection(project).runWithServer(taskId, server ->
                FileToTargetQuery.findDependantTargetsFromPaths(
                    server,
                    filePaths,
                    snapshot.repoMapping(),
                    taskId
                )
            ))
            .<@Nullable Map<Path, List<Label>>>thenApply(targetsByPath -> {
                Map<Path, List<Label>> result = new LinkedHashMap<>();
                targetsByPath.forEach((path, targets) -> {
                    if (!targets.isEmpty()) {
                        result.put(path, targets);
                    }
                });
                return result;
            })
            .exceptionally(error -> {
                Throwable ex = unwrap(error);
                // consulo: upstream rethrowControlFlowException(ex)
                if (ex instanceof ControlFlowException || ex instanceof CancellationException) {
                    throw new CompletionException(ex);
                }
                LOG.debug(ex);
                return null;
            });
    }

    private static Throwable unwrap(Throwable error) {
        Throwable result = error;
        while (result instanceof CompletionException && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    // consulo: kotlin.random.Random.nextBytes(n).toHexString()
    private static String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        new Random().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
