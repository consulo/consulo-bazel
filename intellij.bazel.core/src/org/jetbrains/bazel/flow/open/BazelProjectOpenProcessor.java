package org.jetbrains.bazel.flow.open;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.BazelProjectStoreService;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModifiableModuleModel;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.project.Project;
import consulo.project.startup.StartupActivity;
import consulo.project.startup.StartupManager;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.utils.PathUtil;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject;

/**
 * Refrain from using {@link VirtualFile#getChildren} as it causes performance issues in large projects, such as <a href="https://youtrack.jetbrains.com/issue/BAZEL-1717">BAZEL-1717</a>
 */
// consulo: IntelliJ's ProjectOpenProcessor is internal in Consulo; opening a Bazel workspace goes through the import of the
// Consulo open flow (ModuleImportProvider). The project is opened by the platform, the processor records the Bazel store
// descriptor (upstream: ProjectStorePathCustomizer) and runs the upstream open callback after the project is opened.
@ExtensionImpl
public class BazelProjectOpenProcessor implements ModuleImportProvider<ModuleImportContext> {
    private static final Logger LOG = Logger.getInstance(BazelProjectOpenProcessor.class);

    @Override
    public Coroutine<Object, Object> process(
        ModuleImportContext context,
        Project project,
        ModifiableModuleModel model,
        Consumer<Module> newModuleConsumer
    ) {
        return CodeExecution.run(continuation -> {
            Path openedFile = Path.of(context.getFileToImport());
            LOG.info("Opening asynchronously file as a Bazel project: " + openedFile);

            // The user (or another part of the plugin) may try to open the project by pointing to:
            // BUILD files, MODULE.bazel, project view file, etc. so we need to do normalization here
            utils.ResolvedProjectRoot resolvedProjectRoot = utils.ResolvedProjectRoot.from(openedFile);
            if (resolvedProjectRoot == null) {
                return;
            }
            Path projectRootDir = resolvedProjectRoot.getRootDir();
            Path fileBeingOpen = resolvedProjectRoot.getOpenedFile();
            LOG.trace("Found project root directory: " + projectRootDir);

            Path projectViewFile = pickProjectViewFileForProject(fileBeingOpen, projectRootDir);
            LOG.trace("Using project view file: " + projectViewFile);

            // consulo: upstream opens the project view file with createModule = false; the store descriptor that
            // BazelProjectStorePathCustomizer computes for it marks the project as a Bazel project
            BazelProjectStoreDescriptor descriptor = new BazelProjectStorePathCustomizer().getStoreDirectoryPath(projectViewFile);
            if (descriptor == null) {
                return;
            }
            BazelProjectStoreService.getInstance(project).setDescriptor(descriptor);

            // consulo: upstream OpenProjectTask callback
            StartupManager.getInstance(project).runAfterOpened((StartupActivity) (openedProject, uiAccess) -> {
                BazelCoroutineService.getInstance(project).start(() -> {
                    VirtualFile projectViewPath = PathUtil.refreshAndFindVirtualFile(projectViewFile);
                    if (projectViewPath != null) {
                        return utils.openProjectViewInEditor(project, projectViewPath);
                    }
                    return CompletableFuture.completedFuture(null);
                });

                // force async load targets cache
                project.getInstance(TargetStorage.class);
                project.getInstance(ModuleTargetService.class);
            });
        }).toCoroutine();
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(BazelPluginConstants.BAZEL_DISPLAY_NAME);
    }

    // consulo: upstream isStrongProjectInfoHolder (BazelFeatureFlags.autoOpenProjectIfPresent) has no Consulo counterpart

    /**
     * The project is only eligible to be opened with Bazel Plugin if workspace files can be reached from the given vFile
     */
    @Override
    public boolean canImport(Path file) {
        // consulo: upstream skips directories with an existing .idea unless autoOpenProjectIfPresent is set; Consulo itself asks
        // whether to open the existing project or to import it again
        return utils.findProjectFolderFromFile(file) != null;
    }

    @Override
    public String getPathToBeImported(Path file) {
        return file.toString();
    }
}
