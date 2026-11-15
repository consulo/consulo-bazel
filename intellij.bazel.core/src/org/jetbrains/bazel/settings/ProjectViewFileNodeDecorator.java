package org.jetbrains.bazel.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.project.DefaultProjectViewService;

@ExtensionImpl
public class ProjectViewFileNodeDecorator implements ProjectViewNodeDecorator {
    private final Project myProject;

    @Inject
    public ProjectViewFileNodeDecorator(Project project) {
        myProject = project;
    }

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            return;
        }
        VirtualFile vFile = node.getVirtualFile();
        if (vFile == null) {
            return;
        }
        if (!isProjectViewFile(vFile)) {
            return;
        }
        if (!DefaultProjectViewService.getProjectViewFile(myProject).equals(vFile)) {
            return;
        }
        data.addText(vFile.getName() + "  ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        data.addText(BazelPluginLocalize.projectViewTreeProjectViewFileHint(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private boolean isProjectViewFile(VirtualFile receiver) {
        return Constants.PROJECT_VIEW_FILE_EXTENSION.equals(receiver.getExtension());
    }
}
