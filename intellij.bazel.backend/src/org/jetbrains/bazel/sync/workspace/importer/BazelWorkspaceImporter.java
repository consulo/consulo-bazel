package org.jetbrains.bazel.sync.workspace.importer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.component.extension.ExtensionPoint;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.progress.TaskConsole;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.GlobalNamingContextBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.TaskId;

import java.util.concurrent.CompletableFuture;

/**
 * Extensible workspace importer, main purpose of {@link BazelWorkspaceImporter}
 * is to act as transform function from {@link WorkspaceSnapshot} to an actual IDE native
 * project model.
 * <p>
 * Currently {@link BazelWorkspaceImporter} support pure transformation to {@link BazelModelBuilder} aka. the project model.
 * Each importer can inspect full scope of {@link WorkspaceSnapshot}, but shouldn't change its own behavior depending
 * on external factors like other language data that its own.
 * <p>
 * <b>NOTE:</b> Each {@link BazelWorkspaceImporter} should aim to be a pure function,
 * importers should only realy on {@link WorkspaceSnapshot} as a source of truth for import input data
 * <p>
 * <b>RECOMMENDATION:</b> Each {@link BazelWorkspaceImporter} should import exactly single language/technology,
 * in case of importer outputing model being shared among multiple languages/technologies, it's
 * importer responsiblity to provide extensions for extending its own behavior.
 * <p>
 * consulo: the IntelliJ workspace model ({@code MutableEntityStorage}) is replaced by the classic project model
 * through {@link BazelModelBuilder}.
 */
public interface BazelWorkspaceImporter {
    /**
     * Internalized {@link BazelWorkspaceImporter} importer name
     */
    LocalizeValue getImporterName();

    /**
     * Perform phased workspace import. Adhear to rules found in {@link WorkspaceImporterPhase} documentation.
     *
     * @param phase    Current importer phase
     * @param snapshot Immutable workspace snapshot
     * @param taskId   Parent {@link TaskId} of the current phase for this importer
     * @return the result of the phase. The importer throws an exception when the phase fails.
     */
    CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId);

    /**
     * Factory for creating {@link BazelWorkspaceImporter},
     * workspace importer instance is created at start of each sync
     */
    @ExtensionAPI(ComponentScope.APPLICATION)
    interface BazelWorkspaceImporterFactory {
        BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context);

        static ExtensionPoint<BazelWorkspaceImporterFactory> getEpName() {
            return Application.get().getExtensionPoint(BazelWorkspaceImporterFactory.class);
        }
    }

    /**
     * Success result of {@link BazelWorkspaceImporter#doImport}
     */
    enum WorkspaceImporterResult {
        /**
         * Success {@link BazelWorkspaceImporter} execution shall be continued
         */
        Success,
        /**
         * Abort {@link BazelWorkspaceImporter} execution, skip all next phases
         */
        Abort
    }

    /**
     * Common {@link BazelWorkspaceImporter} context
     * <p>
     * consulo: upstream {@code vfuManager} and {@code currentSnapshot} are workspace model objects and are not available;
     * the current project model is read through the classic API.
     *
     * @param project          Current project, don't use {@code project} to collect data that can change {@link BazelWorkspaceImporter} behavior
     * @param taskConsole      Current {@link TaskConsole}
     * @param progressReporter Current progress indicator
     */
    record WorkspaceImporterContext(
        Project project,
        TaskConsole taskConsole,
        ProgressIndicator progressReporter,
        OutputLocationResolver outputResolver,
        OutputLocationParser outputParser,
        BazelOutFileHardLinks outFileHardLinks,
        BazelInfo bazelInfo
    ) {
    }

    /**
     * Phases of {@link BazelWorkspaceImporter}
     * <p>
     * Phase call order
     * <pre>
     * Initialize -> WorkspaceApply -> Finalize -> PostProcessing (After workspace model apply)
     * </pre>
     */
    // RC: for now support only monolithic updates to shared `MutableEntityStorage`
    sealed interface WorkspaceImporterPhase
        permits WorkspaceImporterPhase.Initialize, WorkspaceImporterPhase.WorkspaceApply, WorkspaceImporterPhase.Finalize,
        WorkspaceImporterPhase.PostProcessing {

        /**
         * Called first once per {@link BazelWorkspaceImporter#doImport}
         *
         * @param naming The only phase where names can be declared, see {@link GlobalNamingContextBuilder}
         */
        record Initialize(GlobalNamingContextBuilder naming) implements WorkspaceImporterPhase {
        }

        /**
         * Apply changes to shared {@link BazelModelBuilder}, remember that {@code builder} is shared among all
         * {@link BazelWorkspaceImporter} instances.
         * <p>
         * Called exactly once for every {@link BazelWorkspaceImporter}.
         *
         * @param naming Names declared during {@link Initialize}, resolved across all importers
         */
        record WorkspaceApply(
            BazelModelBuilder builder,
            BazelEntitySource entitySource,
            GlobalNamingContext naming
        ) implements WorkspaceImporterPhase {
        }

        /**
         * Called last once per {@link BazelWorkspaceImporter#doImport}
         */
        final class Finalize implements WorkspaceImporterPhase {
            public static final Finalize INSTANCE = new Finalize();

            private Finalize() {
            }
        }

        /**
         * Optional phase called after {@link Finalize}, depending on environment might not be invoked.
         * {@link PostProcessing} phase will be applied after workspace model
         * application if concept of workspace model application is present in invocation environment.
         */
        final class PostProcessing implements WorkspaceImporterPhase {
            public static final PostProcessing INSTANCE = new PostProcessing();

            private PostProcessing() {
            }
        }
    }
}
