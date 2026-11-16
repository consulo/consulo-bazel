package org.jetbrains.bazel.sync.workspace.importer;

import consulo.application.progress.ProgressIndicator;
import consulo.bazel.backend.localize.BazelBackendLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterPhase;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterResult;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consulo: the workspace model builder (MutableEntityStorage) is the classic BazelModelBuilder;
// suspend functions return CompletableFuture, importers of a phase run one after another as upstream
public class WorkspaceImporterHelper {
    private final Project myProject;
    private final TaskConsole myTaskConsole;
    private final ProgressIndicator myProgressReporter;
    private final BazelModelBuilder myBuilder;
    private final OutputLocationResolver myOutputResolver;
    private final OutputLocationParser myOutputParser;
    private final BazelOutFileHardLinks myOutFileHardLinks;
    private final BazelInfo myBazelInfo;

    private final Set<BazelWorkspaceImporter> myToSkip = new HashSet<>();
    private List<BazelWorkspaceImporter> myImporters = List.of();

    private final Map<String, Long> myImporterToTime = new HashMap<>();

    /**
     * Time spent by each {@link BazelWorkspaceImporter} in milliseconds
     */
    public Map<String, Long> getTimePerImporter() {
        return myImporterToTime;
    }

    public WorkspaceImporterHelper(
        Project project,
        TaskConsole taskConsole,
        ProgressIndicator progressReporter,
        BazelModelBuilder builder,
        OutputLocationResolver outputResolver,
        OutputLocationParser outputParser,
        BazelOutFileHardLinks outFileHardLinks,
        BazelInfo bazelInfo
    ) {
        myProject = project;
        myTaskConsole = taskConsole;
        myProgressReporter = progressReporter;
        myBuilder = builder;
        myOutputResolver = outputResolver;
        myOutputParser = outputParser;
        myOutFileHardLinks = outFileHardLinks;
        myBazelInfo = bazelInfo;
    }

    public CompletableFuture<Void> invoke(ProgressIndicator reporter, WorkspaceSnapshot snapshot, TaskId taskId) {
        WorkspaceImporterContext context = new WorkspaceImporterContext(
            myProject,
            myTaskConsole,
            myProgressReporter,
            myOutputResolver,
            myOutputParser,
            myOutFileHardLinks,
            myBazelInfo
        );
        List<BazelWorkspaceImporter> importers = new ArrayList<>();
        BazelWorkspaceImporterFactory.getEpName().forEachExtensionSafe(it -> importers.add(it.createWorkspaceImporter(context)));
        myImporters = importers;
        GlobalNamingContextBuilder namingBuilder = GlobalNamingContextBuilder.create(snapshot.repoMapping());
        return util.withSubtask(
            myTaskConsole, reporter, taskId.subTask("workspace-importers"),
            BazelBackendLocalize.bazelWorkspaceImporterTaskName(),
            importersTaskId -> util.<Void>withSubtask(
                    myTaskConsole,
                    importersTaskId.subTask("workspace-importers-init"),
                    BazelBackendLocalize.workspaceImporterPhaseInitializationProgress(),
                    initTaskId -> forEachImporter(importer ->
                        runContextual(importer, initTaskId, new WorkspaceImporterPhase.Initialize(namingBuilder), snapshot)
                            .thenAccept(result -> {
                                if (result == null || result == WorkspaceImporterResult.Abort) {
                                    myToSkip.add(importer);
                                }
                            })
                    )
                )
                .thenCompose(ignored -> {
                    GlobalNamingContext naming = namingBuilder.build();
                    return util.<Void>withSubtask(
                        myTaskConsole,
                        importersTaskId.subTask("workspace-importers-wsm-building"),
                        BazelBackendLocalize.buildWorkspaceModel(),
                        applyTaskId -> forEachImporter(importer -> {
                            if (myToSkip.contains(importer)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            WorkspaceImporterPhase phase = new WorkspaceImporterPhase.WorkspaceApply(
                                myBuilder,
                                BazelEntitySource.BazelProjectEntitySource.INSTANCE,
                                naming
                            );
                            return runContextual(importer, applyTaskId, phase, snapshot).thenAccept(result -> {
                                if (result == null || result == WorkspaceImporterResult.Abort) {
                                    myToSkip.add(importer);
                                }
                            });
                        })
                    );
                })
                .thenCompose(ignored -> util.<Void>withSubtask(
                    myTaskConsole,
                    importersTaskId.subTask("workspace-importers-finalize"),
                    BazelBackendLocalize.workspaceImporterPhaseFinalization(),
                    finalizeTaskId -> forEachImporter(importer -> {
                        if (myToSkip.contains(importer)) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return runContextual(importer, finalizeTaskId, WorkspaceImporterPhase.Finalize.INSTANCE, snapshot)
                            .thenAccept(result -> {
                                if (result == WorkspaceImporterResult.Abort) {
                                    myToSkip.add(importer);
                                }
                            });
                    })
                ))
        );
    }

    public CompletableFuture<Void> invokeLate(ProgressIndicator reporter, WorkspaceSnapshot snapshot, TaskId taskId) {
        return util.withSubtask(
            myTaskConsole, reporter, taskId.subTask("workspace-importers-post-apply"),
            BazelBackendLocalize.bazelWorkspacePostApplyTaskName(),
            postTaskId -> forEachImporter(importer -> {
                if (myToSkip.contains(importer)) {
                    return CompletableFuture.completedFuture(null);
                }
                return runContextual(importer, postTaskId, WorkspaceImporterPhase.PostProcessing.INSTANCE, snapshot)
                    .thenAccept(result -> {
                    });
            })
        );
    }

    // consulo: importers.forEach with a suspending body, one importer after another
    private CompletableFuture<Void> forEachImporter(Function<BazelWorkspaceImporter, CompletableFuture<Void>> action) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (BazelWorkspaceImporter importer : myImporters) {
            chain = chain.thenCompose(ignored -> action.apply(importer));
        }
        return chain;
    }

    // MAYBE RC: using context parameters for `TaskId` would be great fit here
    private CompletableFuture<@Nullable WorkspaceImporterResult> runContextual(
        BazelWorkspaceImporter importer,
        TaskId taskId,
        WorkspaceImporterPhase phase,
        WorkspaceSnapshot snapshot
    ) {
        return util.isolateSubtaskFailure(myTaskConsole, taskId, () -> {
            long startTime = System.currentTimeMillis();
            CompletableFuture<WorkspaceImporterResult> result;
            try {
                result = util.withSubtask(
                    myTaskConsole,
                    taskId.uniqueSubTask("importer"),
                    BazelBackendLocalize.workspaceImporterPhaseExecuting(importer.getImporterName()),
                    importerTaskId -> importer.doImport(phase, snapshot, importerTaskId)
                );
            }
            catch (Throwable e) {
                result = CompletableFuture.failedFuture(e);
            }
            // consulo: the upstream finally block runs when the future completes
            return result.whenComplete((value, error) -> {
                long endTime = System.currentTimeMillis();
                String importerName = importer.getClass().getName();
                myImporterToTime.merge(importerName, endTime - startTime, Long::sum);
            });
        });
    }
}
