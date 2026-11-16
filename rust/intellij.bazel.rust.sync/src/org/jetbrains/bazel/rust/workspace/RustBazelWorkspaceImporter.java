package org.jetbrains.bazel.rust.workspace;

import consulo.bazel.rust.localize.BazelRustLocalize;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.progress.util;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.model.ExternalCargoProjectData;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Imports the Rust crates of the workspace: a module whose content roots are the Bazel packages of the crates, and the crates
 * themselves as an external Cargo project of consulo-rust ({@link CargoProjectsService#attachExternalCargoProject}).
 * <p>
 * consulo: Consulo-only, upstream never had an IDE side for the Bazel Rust support (the BSP server answered
 * {@code buildTarget/rustWorkspace} for intellij-rust, removed in 4699fc65a). consulo-rust does not persist external projects:
 * {@link RustCargoProjectStartupActivity} attaches the project again when the IDE project is opened.
 */
public class RustBazelWorkspaceImporter implements BazelWorkspaceImporter {
    private static final Logger LOG = Logger.getInstance(RustBazelWorkspaceImporter.class);

    public static final String RUST_WORKSPACE_MODULE_NAME = ".rust-workspace";

    private final WorkspaceImporterContext myContext;

    private @Nullable BazelRustWorkspace myWorkspace;

    public RustBazelWorkspaceImporter(WorkspaceImporterContext context) {
        myContext = context;
    }

    @Override
    public LocalizeValue getImporterName() {
        return BazelRustLocalize.consoleTaskModelRustImporter();
    }

    @Override
    public CompletableFuture<WorkspaceImporterResult> doImport(WorkspaceImporterPhase phase, WorkspaceSnapshot snapshot, TaskId taskId) {
        // an exception of a failed phase -> a failed future, like the other importers
        try {
            if (phase instanceof WorkspaceImporterPhase.Initialize) {
                BazelRustWorkspace workspace = BazelRustWorkspace.of(snapshot);
                if (workspace.isEmpty()) {
                    // no Rust crates (anymore): the project of a previous sync is dropped
                    detachExternalCargoProjects(myContext.project());
                    return CompletableFuture.completedFuture(WorkspaceImporterResult.Abort);
                }
                myWorkspace = workspace;
            }
            else if (phase instanceof WorkspaceImporterPhase.WorkspaceApply apply) {
                addRustWorkspaceModule(apply.builder());
            }
            else if (phase instanceof WorkspaceImporterPhase.PostProcessing) {
                return attachCargoProject(taskId).thenApply(ignored -> WorkspaceImporterResult.Success);
            }
            return CompletableFuture.completedFuture(WorkspaceImporterResult.Success);
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * The crate sources have to be under a content root to be indexed, like the Go workspace module. consulo-rust needs no source
     * roots and creates no modules for an external project: the standard library and dependency crates become order entries of the
     * module holding the Bazel workspace root.
     */
    private void addRustWorkspaceModule(BazelModelBuilder builder) {
        BazelRustWorkspace workspace = myWorkspace;
        if (workspace == null) {
            return;
        }
        List<Path> directories = workspace.getWorkspaceCrateDirectories();
        if (directories.isEmpty()) {
            return;
        }
        // the module source dependency is implicit, the module is owned by the builder
        Module module = builder.getOrCreateModule(RUST_WORKSPACE_MODULE_NAME, null);
        ModifiableRootModel model = builder.getRootModel(module);
        for (Path directory : directories) {
            model.addContentEntry(VirtualFileUtil.pathToUrl(directory.toString()));
        }
    }

    // after the project model is committed: the sync of consulo-rust adds its order entries to the committed modules
    private CompletableFuture<Void> attachCargoProject(TaskId taskId) {
        BazelRustWorkspace workspace = myWorkspace;
        if (workspace == null) {
            return CompletableFuture.completedFuture(null);
        }
        Project project = myContext.project();
        return util.withSubtask(
            util.getSyncConsole(project),
            myContext.progressReporter(),
            taskId.subTask("attach-rust-workspace"),
            BazelRustLocalize.consoleTaskModelAttachRustWorkspace(),
            subtaskId -> attach(project, workspace.toExternalCargoProjectData())
        );
    }

    /**
     * Attaches the crates to consulo-rust, or replaces the data of the project attached by the previous sync or project opening.
     */
    public static CompletableFuture<Void> attach(Project project, ExternalCargoProjectData data) {
        LOG.info("Attaching the Bazel Rust workspace " + data);
        return CargoProjectsService.getInstance(project).attachExternalCargoProject(data).thenApply(ignored -> null);
    }

    private static void detachExternalCargoProjects(Project project) {
        CargoProjectsService service = CargoProjectsService.getInstance(project);
        for (CargoProject cargoProject : List.copyOf(service.getAllProjects())) {
            if (cargoProject.isExternal() && isBazelManifest(cargoProject.getManifest())) {
                service.detachCargoProject(cargoProject);
            }
        }
    }

    private static boolean isBazelManifest(Path manifest) {
        Path fileName = manifest.getFileName();
        if (fileName == null) {
            return false;
        }
        String name = fileName.toString();
        return name.equals("MODULE.bazel") || name.equals("WORKSPACE.bazel") || name.equals("WORKSPACE");
    }
}
