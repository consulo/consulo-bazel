package org.jetbrains.bazel.workspace.importer;

import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.scala.sdk.ScalaSdk;
import org.jetbrains.bazel.scala.sdk.ScalaSdkExtension;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget.ScalaBuildTarget;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ScalaBazelWorkspaceImporter implements BazelWorkspaceImporter {
    // consulo: the upstream nested `Factory` is the top-level ScalaBazelWorkspaceImporterFactory (an @ExtensionImpl cannot be nested)

    private final WorkspaceImporterContext myContext;

    private @Nullable Set<ScalaSdk> myScalaSdks = null;

    public ScalaBazelWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    public WorkspaceImporterContext getContext() {
        return myContext;
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        if (phase instanceof WorkspaceImporterPhase.Initialize) {
            if (!ScalaSdkExtension.scalaSdkExtensionExists()) {
                return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
            }
            myScalaSdks = calculateAllScalaSdkInfos(myContext, snapshot);
        }
        else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
            // consulo: upstream adds the Scala SDKs in PostProcessing through its own IdeModifiableModelsProviderImpl, committed in
            // edtWriteAction. A separate BazelModelBuilder would remove the Bazel-owned modules of this sync on commit, so the SDKs
            // are added to the shared builder of WorkspaceApply, which is committed together with the project model.
            ScalaSdkExtension extension = ScalaSdkExtension.scalaSdkExtension();
            if (extension != null && myScalaSdks != null) {
                for (ScalaSdk it : myScalaSdks) {
                    extension.addScalaSdk(it, apply.builder());
                }
            }
        }
        else if (phase instanceof WorkspaceImporterPhase.PostProcessing) {
            // consulo: the Scala SDKs are added in WorkspaceApply (see above)
        }
        else {
            /* noop */
        }

        return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
    }

    private Set<ScalaSdk> calculateAllScalaSdkInfos(WorkspaceImporterContext context, WorkspaceSnapshot snapshot) {
        LocalRepositoryMapping localRepositories = RepoMapping.getLocalRepositories(snapshot.repoMapping());
        Set<ScalaSdk> result = new LinkedHashSet<>();
        snapshot.targets().allTargets().forEach(target -> {
            ScalaSdk sdk = createScalaSdk(target, it -> context.outputResolver().resolve(it, localRepositories));
            if (sdk != null) {
                result.add(sdk);
            }
        });
        return result;
    }

    private static @Nullable ScalaSdk createScalaSdk(BuildTarget target, Function<OutputLocation, @Nullable Path> resolveLocation) {
        ScalaBuildTarget scalaBuildTarget = JvmBuildTarget.extractScalaBuildTarget(target);
        if (scalaBuildTarget == null) {
            return null;
        }
        return new ScalaSdk(
            JvmTargetEntitiesBuilder.scalaVersionToScalaSdkName(scalaBuildTarget.scalaVersion()),
            scalaBuildTarget.scalaVersion(),
            JvmTargetEntitiesBuilder.resolvePaths(scalaBuildTarget.sdkJars(), resolveLocation).stream().map(Path::toUri).toList()
        );
    }
}
