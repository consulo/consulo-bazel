package org.jetbrains.bazel.ui.status;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorTabColorProvider;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.fileEditor.FileEditor;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.util.LightDarkColorValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.bazel.action.registered.OpenProjectViewAction;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.ignore.BazelIgnoreService;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.sync.status.SyncStatusService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

// consulo: upstream file-level declarations of BazelFileStatusDecorator.kt; BazelFileStatusRefresher (a public service)
// lives in BazelFileStatusRefresher.java
public final class BazelFileStatusDecorator {
    // consulo: JBColor(light, dark) -> LightDarkColorValue
    static final ColorValue IGNORE_COLOR = new LightDarkColorValue(new RGBColor(159, 107, 0), new RGBColor(159, 107, 0));
    static final ColorValue UNSYNCED_BACKGROUND_COLOR = new LightDarkColorValue(new RGBColor(252, 234, 234), new RGBColor(94, 56, 56));

    private BazelFileStatusDecorator() {
    }

    @RequiredReadAction
    public static boolean showAsUnsyncedSourceFile(Project project, VirtualFile file) {
        if (file.isDirectory()) {
            return false;
        }
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        if (LanguageClass.fromExtension(extension) == null) {
            return false;
        }
        try {
            if (!file.toNioPath().startsWith(org.jetbrains.bazel.config.project.getRootDir(project).toNioPath())) {
                return false;
            }
        }
        catch (UnsupportedOperationException ignored) {
            return false;
        }

        if (SyncStatusService.isSyncInProgress(project)) {
            return false;
        }
        if (!BazelFileEventProcessor.getInstance(project).isIdle()) {
            return false;
        }
        if (BazelIgnoreService.getInstance(project).isIgnored(file)) {
            return false;
        }

        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        boolean isExcluded = projectFileIndex.isExcluded(file);
        if (isExcluded) {
            return false;
        }

        // consulo: upstream isUnderSourceRootOfType(file, setOf(JavaResourceRootType.RESOURCE, JavaResourceRootType.TEST_RESOURCE))
        boolean isResource = projectFileIndex.isInResource(file) || projectFileIndex.isInTestResource(file);
        if (isResource) {
            return false;
        }

        TargetStorage targetStorage = TargetStorage.getTargetStorage(project);
        if (targetStorage.getTotalTargetCount() == 0) {
            return false; // not loaded yet
        }

        return targetStorage.getTargetsForFile(file).isEmpty();
    }

    public static boolean showAsIgnoredSourceFile(Project project, VirtualFile file) {
        if (SyncStatusService.isSyncInProgress(project)) {
            return false;
        }
        return BazelIgnoreService.getInstance(project).isIgnored(file);
    }
}

@ExtensionImpl
class BazelSourceFileEditorTabColorProvider implements EditorTabColorProvider {
    public BazelSourceFileEditorTabColorProvider() {
    }

    @Override
    @RequiredReadAction
    public @Nullable ColorValue getEditorTabColor(Project project, VirtualFile file) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        if (!BazelFeatureFlags.getHighlightUnsyncedSourceFiles()) {
            return null;
        }
        return BazelFileStatusDecorator.showAsUnsyncedSourceFile(project, file) ? BazelFileStatusDecorator.UNSYNCED_BACKGROUND_COLOR : null;
    }
}

@ExtensionImpl
class BazelSourceFileEditorTabTitleProvider implements EditorTabTitleProvider {
    public BazelSourceFileEditorTabTitleProvider() {
    }

    @RequiredReadAction
    private @Nullable String getEditorTabTitleImpl(Project project, VirtualFile file) {
        if (BazelFileStatusDecorator.showAsUnsyncedSourceFile(project, file)) {
            return file.getPresentableName() + " " + BazelPluginLocalize.syncStatusUnsyncedSourceFileHint().get();
        }
        else {
            return null;
        }
    }

    // consulo: no EditorTabTitleProvider.getEditorTabTitleAsync()
    //override suspend fun getEditorTabTitleAsync(project: Project, file: VirtualFile): String? {
    //  if (!project.isBazelProject) return null
    //  if (!BazelFeatureFlags.highlightUnsyncedSourceFiles) return null
    //  return readAction {
    //    getEditorTabTitleImpl(project, file)
    //  }
    //}

    @Override
    public @Nullable String getEditorTabTitle(Project project, VirtualFile file) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        if (!BazelFeatureFlags.getHighlightUnsyncedSourceFiles()) {
            return null;
        }
        // consulo: no ReadAction.computeCancellable()
        return ReadAction.compute(() -> getEditorTabTitleImpl(project, file));
    }
}

@ExtensionImpl
class BazelSourceFileNotificationProvider implements EditorNotificationProvider {
    private final Project myProject;

    @Inject
    public BazelSourceFileNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "BazelSourceFileNotificationProvider";
    }

    // consulo: upstream collectNotificationData(project, file) returning a panel factory
    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(
        VirtualFile file,
        FileEditor fileEditor,
        Supplier<EditorNotificationBuilder> builderFactory
    ) {
        Project project = myProject;
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        if (!BazelFeatureFlags.getHighlightUnsyncedSourceFiles()) {
            return null;
        }
        if (!BazelFileStatusDecorator.showAsUnsyncedSourceFile(project, file)) {
            return null;
        }
        return unsyncedSourceFileEditorPanel(project, fileEditor, builderFactory.get());
    }

    // consulo: upstream private class UnsyncedSourceFileEditorPanel (EditorNotificationPanel) fills the Consulo builder
    private static EditorNotificationBuilder unsyncedSourceFileEditorPanel(
        Project project,
        FileEditor fileEditor,
        EditorNotificationBuilder builder
    ) {
        builder.asWarning();
        builder.withText(BazelPluginLocalize.syncStatusUnsyncedSourceFileNotification());
        builder.withAction(
            BazelPluginLocalize.widgetConfigFilePopupMessage(BazelPluginLocalize.toolWindowGenericConfigFile()),
            e -> BazelCoroutineService.getInstance(project).start(() -> OpenProjectViewAction.openProjectView(project))
        );
        VirtualFile editorFile = fileEditor.getFile();
        Path localFile = toNioPathOrNull(editorFile);
        if (localFile != null) {
            builder.withAction(
                BazelPluginLocalize.syncStatusUnsyncedSourceFileBannerResyncFile(),
                e -> BazelCoroutineService.getInstance(project).start(() ->
                    project.getInstance(ProjectSyncService.class).sync(new ProjectSyncScope.Files(List.of(localFile), false))
                )
            );
        }
        else {
            builder.withAction(
                BazelPluginLocalize.resyncActionText(),
                e -> BazelCoroutineService.getInstance(project).start(() ->
                    project.getInstance(ProjectSyncService.class).sync(new ProjectSyncScope.Full(false, false))
                )
            );
        }
        return builder;
    }

    // consulo: upstream VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }
}

@ExtensionImpl
class BazelSourceFileNodeDecorator implements ProjectViewNodeDecorator {
    private final Project myProject;

    @Inject
    public BazelSourceFileNodeDecorator(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            return;
        }
        VirtualFile vFile = node.getVirtualFile();
        if (vFile != null) {
            if (BazelFeatureFlags.getHighlightIgnoredSourceFiles() && BazelFileStatusDecorator.showAsIgnoredSourceFile(myProject, vFile)) {
                data.setForcedTextForeground(BazelFileStatusDecorator.IGNORE_COLOR);
            }
            else if (BazelFeatureFlags.getHighlightUnsyncedSourceFiles()
                && BazelFileStatusDecorator.showAsUnsyncedSourceFile(myProject, vFile)) {
                data.addText(vFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                data.addText(BazelPluginLocalize.syncStatusUnsyncedSourceFileHint(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
                data.setBackground(BazelFileStatusDecorator.UNSYNCED_BACKGROUND_COLOR);
            }
        }
    }
}
