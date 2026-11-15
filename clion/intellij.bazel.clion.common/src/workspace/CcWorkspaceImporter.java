package org.jetbrains.bazel.clion.workspace;

import consulo.bazel.cpp.localize.BazelCLionCommonLocalize;
import consulo.bazel.cpp.workspace.CcWorkspaceModel;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CcWorkspaceImporter implements BazelWorkspaceImporter, BazelWorkspaceImporter.Named {
    // consulo: the upstream nested `Factory` is the top-level CcWorkspaceImporterFactory (an @ExtensionImpl cannot be nested)

    public static final String CC_CLIENT_KEY = "BAZEL_CC";

    private static final int CLIENT_VERSION = 0;

    private static final Logger LOG = Logger.getInstance(CcWorkspaceImporter.class);

    private final WorkspaceImporterContext myContext;

    private List<CcResolveConfiguration> myConfigurations = List.of();
    private Map<WorkspaceTargetKey, CcCompilerInfo> myToolchain2Compiler = Map.of();

    public CcWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    public WorkspaceImporterContext getContext() {
        return myContext;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelCLionCommonLocalize.ccWorkspaceImporterName();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        if (!BazelCLionFeatureFlags.isCLionEnabled()) {
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
        }

        // consulo: Kotlin Result -> a failed future
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize) {
                return onInitialize(snapshot, taskId);
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                return onWorkspaceApply(apply, snapshot, taskId);
            }
            else if (phase instanceof WorkspaceImporterPhase.PostProcessing) {
                return onPostProcessing(snapshot, taskId);
            }
            else {
                return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
            }
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<WorkspaceImporterResult> onInitialize(WorkspaceSnapshot snapshot, TaskId taskId) {
        return subtask(
            snapshot,
            "cc.import.task.toolchain.map",
            taskId,
            ctx -> CompletableFuture.completedFuture(buildToolchainMap.buildToolchainMap(ctx))
        )
            .thenCompose(target2Toolchain -> {
                if (target2Toolchain.isEmpty()) {
                    return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
                }

                return subtask(snapshot, "cc.import.task.compiler.settings", taskId, buildCompilerSettings::buildCompilerSettings)
                    .thenCompose(toolchain2Compiler -> {
                        Map<WorkspaceTargetKey, @Nullable CcCompilerInfo> target2Compiler = new LinkedHashMap<>();
                        for (Map.Entry<WorkspaceTargetKey, WorkspaceTargetKey> it : target2Toolchain.entrySet()) {
                            target2Compiler.put(it.getKey(), toolchain2Compiler.get(it.getValue()));
                        }

                        return subtask(
                            snapshot,
                            "cc.import.task.equivalence.classes",
                            taskId,
                            ctx -> CompletableFuture.completedFuture(buildEquivalenceClasses.buildEquivalenceClasses(ctx, target2Compiler))
                        ).thenApply(configurations -> {
                            myConfigurations = configurations;
                            myToolchain2Compiler = toolchain2Compiler;

                            return WorkspaceImporterResult.Success;
                        });
                    });
            });
    }

    private CompletableFuture<WorkspaceImporterResult> onWorkspaceApply(
        WorkspaceImporterPhase.WorkspaceApply phase,
        WorkspaceSnapshot snapshot,
        TaskId taskId
    ) {
        BazelModelBuilder builder = phase.builder();
        workspaceModule.addCcWorkspaceModule(builder, phase.entitySource());
        workspaceModule.addCcCompilerInfoEntities(builder, phase.entitySource(), myToolchain2Compiler);

        // consulo: upstream fills the OC workspace in onPostProcessing, after the .cc-workspace module it is stored under
        // exists. Its counterpart, the CcWorkspaceModel, is project data of the builder: it is written here so that the
        // commit publishes it together with the modules, and consulo-cpp indexes the files with their options
        CcWorkspaceModel workspace = new CcWorkspaceModel(CC_CLIENT_KEY);
        // consulo: upstream workspace.setClientVersion(CLIENT_VERSION), consulo-cpp has no client versions
        return subtask(snapshot, "cc.import.task.oc.workspace", taskId, ctx -> {
            buildWorkspaceModel.buildWorkspaceModel(ctx, workspace, myConfigurations);
            return CompletableFuture.completedFuture(null);
        }).thenApply(ignored -> {
            builder.putProjectData(CcWorkspaceModel.class, workspace);
            return WorkspaceImporterResult.Success;
        });
    }

    private CompletableFuture<WorkspaceImporterResult> onPostProcessing(WorkspaceSnapshot snapshot, TaskId taskId) {
        if (workspaceModule.findCcWorkspaceModuleId(myContext.project()) == null) {
            LOG.error("CC workspace module is absent, dropping " + myConfigurations.size() + " configuration(s)");
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
        }

        // consulo: OCWorkspaceImpl.getInstanceImpl(project).getModifiableModel(CC_CLIENT_KEY, clear = true) -> the model
        // published by onWorkspaceApply
        CcWorkspaceModel workspace = CcWorkspaceModel.getInstance(myContext.project());
        if (workspace == null) {
            LOG.error("CC workspace model is absent, dropping " + myConfigurations.size() + " configuration(s)");
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
        }

        return subtask(snapshot, "cc.import.task.compiler.info", taskId, ctx -> {
            collectCompilerInfo.collectCompilerInfo(ctx, workspace, myConfigurations);
            return CompletableFuture.completedFuture(null);
        }).whenComplete((ignored, error) -> {
            // consulo: upstream finally block; workspace.preCommit(), workspace.commitAndContribute() and
            // Disposer.dispose(workspace) are not needed, the model was committed with the project model
            myConfigurations = List.of();
            myToolchain2Compiler = Map.of();
        }).thenApply(ignored -> WorkspaceImporterResult.Success);
    }

    private <T> CompletableFuture<T> subtask(
        WorkspaceSnapshot snapshot,
        String key,
        TaskId parentTaskId,
        Function<CcImportContext, CompletableFuture<T>> body
    ) {
        return util.withSubtask(
            myContext.taskConsole(),
            parentTaskId.subTask(key),
            // consulo: the key is a key of BazelCLionCommonLocalize
            LocalizeKey.of(BazelCLionCommonLocalize.ID, key).getValue(),
            taskId -> {
                CcImportContext taskCtx = CcImportContext.create(taskId, myContext, snapshot);
                return body.apply(taskCtx);
            }
        );
    }
}
