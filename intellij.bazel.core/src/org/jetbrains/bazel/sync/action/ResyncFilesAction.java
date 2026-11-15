package org.jetbrains.bazel.sync.action;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// consulo: upstream sealed class; not sealed because the registered ResyncFilesActionXmlRegistered extends XmlRegistered
public abstract class ResyncFilesAction extends BazelSyncAction {
    public static boolean isResyncable(VirtualFile receiver) {
        if (receiver.isDirectory()) {
            for (String it : Constants.BUILD_FILE_NAMES) {
                if (receiver.findChild(it) != null) {
                    return true;
                }
            }
        }
        return Arrays.asList(Constants.BUILD_FILE_NAMES).contains(receiver.getName());
    }

    protected ResyncFilesAction() {
        super(BazelPluginLocalize.resyncFilesAndDirectoriesActionText(), BazelPluginIcons.bazelReload());
    }

    protected abstract List<VirtualFile> selectedFiles(AnActionEvent e);

    private List<VirtualFile> resyncableFiles(AnActionEvent e) {
        List<VirtualFile> result = new ArrayList<>();
        for (VirtualFile it : selectedFiles(e)) {
            if (isResyncable(it)) {
                result.add(it);
            }
        }
        return result;
    }

    @Override
    protected @Nullable ProjectSyncScope computeScope(Project project, AnActionEvent e) {
        Set<Path> paths = new LinkedHashSet<>();
        for (VirtualFile it : resyncableFiles(e)) {
            Path path = toNioPathOrNull(it);
            if (path != null) {
                paths.add(path);
            }
        }
        if (paths.isEmpty()) {
            return null;
        }
        else {
            return new ProjectSyncScope.Files(/* files = */ new ArrayList<>(paths), /* build = */ false);
        }
    }

    @Override
    protected boolean shouldBeVisible(Project project, AnActionEvent e) {
        return !resyncableFiles(e).isEmpty();
    }

    @Override
    protected void updatePresentation(Project project, AnActionEvent e) {
        List<VirtualFile> files = resyncableFiles(e);
        int directories = (int) files.stream().filter(VirtualFile::isDirectory).count();
        if (directories == files.size()) {
            e.getPresentation().setText(BazelPluginLocalize.resyncDirectoriesActionText(files.size()));
        }
        else if (directories == 0) {
            e.getPresentation().setText(BazelPluginLocalize.resyncFilesActionText(files.size()));
        }
        else {
            e.getPresentation().setText(BazelPluginLocalize.resyncFilesAndDirectoriesActionText());
        }
    }

    // consulo: IntelliJ VirtualFile.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(VirtualFile file) {
        try {
            return file.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * consulo: registered through the top-level {@link ResyncFilesActionXmlRegistered} (annotation processor limitation)
     */
    public static class XmlRegistered extends ResyncFilesAction {
        @Override
        protected List<VirtualFile> selectedFiles(AnActionEvent e) {
            VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
            return files != null ? Arrays.asList(files) : List.of();
        }
    }

    public static class NonXmlRegistered extends ResyncFilesAction {
        private final List<VirtualFile> myFiles;

        public NonXmlRegistered(List<VirtualFile> files) {
            myFiles = files;
        }

        @Override
        protected List<VirtualFile> selectedFiles(AnActionEvent e) {
            return myFiles;
        }
    }
}
