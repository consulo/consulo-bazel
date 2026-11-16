package org.jetbrains.bazel.clion.run.debug;

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.bazel.cpp.localize.BazelCLionCoreLocalize;
import consulo.build.ui.event.MessageEvent;
import consulo.process.ExecutionException;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

// consulo: top-level declarations of upstream collectTargetInfo.kt
public final class collectTargetInfo {
    private collectTargetInfo() {
    }

    // consulo: upstream public top-level data class of collectTargetInfo.kt
    public record CcDebugTargetInfo(
        BuildTarget target,
        RepoMapping repoMapping
    ) {
    }

    /**
     * Finds the target in the current workspace snapshot.
     *
     * @param targets                    the targets of the debug build, null if the debug aspect did not run
     * @param reportUnknownConfiguration false skips the warning for a configuration that the snapshot does not contain
     */
    // consulo: @Throws(ExecutionException::class) context(ctx: CcDebugContext) suspend fun -> ctx parameter, the future fails with the
    //   ExecutionException
    static CompletableFuture<CcDebugTargetInfo> collectTargetInfo(
        CcDebugContext ctx,
        Collection<TargetIdeInfo> targets,
        boolean reportUnknownConfiguration
    ) {
        return ctx.getProject().getInstance(WorkspaceSnapshotService.class).currentSnapshot().thenApply(snapshot -> {
            BuildTarget target = findSnapshotTarget(ctx, snapshot, targets, reportUnknownConfiguration);
            if (target == null) {
                throw new CompletionException(new ExecutionException(BazelCLionCoreLocalize.debugErrorTargetNotFound(ctx.getLabel())));
            }

            return new CcDebugTargetInfo(target, snapshot.repoMapping());
        });
    }

    /**
     * Finds the target in the configuration that the debug build used, because the snapshot can contain a label in more than one
     * configuration. Uses a different configuration if the snapshot does not contain this configuration, and reports a warning
     * if {@code reportUnknownConfiguration} is true.
     * <p>
     * For example, the debug_flags project view section can change the configuration.
     */
    // consulo: context(ctx: CcDebugContext) -> ctx parameter
    public static @Nullable BuildTarget findSnapshotTarget(
        CcDebugContext ctx,
        WorkspaceSnapshot snapshot,
        @Nullable Collection<TargetIdeInfo> targets,
        boolean reportUnknownConfiguration
    ) {
        List<BuildTarget> candidates = WorkspaceHelpers.getAllTargets(snapshot)
            .filter(it -> it.getKey().label().equals(ctx.getLabel()))
            .toList();

        // without the configuration of the debug build, any configuration is as good as the other
        TargetIdeInfo debugTarget = targets == null ? null : buildExecutable.findDebugTarget(targets, ctx.getLabel());
        if (debugTarget == null) {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        WorkspaceConfigurationId configuration = WorkspaceConfigurationId.of(debugTarget.getKey().getConfiguration());

        for (BuildTarget it : candidates) {
            if (it.getKey().configuration().equals(configuration)) {
                return it;
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        BuildTarget fallback = candidates.get(0);
        if (!reportUnknownConfiguration) {
            return fallback;
        }

        ctx.reportEvent(
            /* severity = */ MessageEvent.Kind.WARNING,
            /* message = */ BazelCLionCoreLocalize.debugWarningTargetConfigurationUnknown(ctx.getLabel()),
            /* description = */ BazelCLionCoreLocalize.debugWarningTargetConfigurationUnknownDescription(ctx.getLabel()).get()
        );

        return fallback;
    }

    // consulo: upstream default value `reportUnknownConfiguration: Boolean = true`
    public static @Nullable BuildTarget findSnapshotTarget(
        CcDebugContext ctx,
        WorkspaceSnapshot snapshot,
        @Nullable Collection<TargetIdeInfo> targets
    ) {
        return findSnapshotTarget(ctx, snapshot, targets, true);
    }
}
