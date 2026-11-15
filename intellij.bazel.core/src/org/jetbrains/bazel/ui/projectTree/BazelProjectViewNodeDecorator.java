package org.jetbrains.bazel.ui.projectTree;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.bazel.assets.BazelPluginIcons;

import java.util.Arrays;

import static org.jetbrains.bazel.commons.constants.Constants.SUPPORTED_CONFIG_FILE_NAMES;

@ExtensionImpl
class BazelProjectViewNodeDecorator implements ProjectViewNodeDecorator {
    private final Project myProject;

    @Inject
    public BazelProjectViewNodeDecorator(Project project) {
        myProject = project;
    }

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            decorateForBsp(node, data);
        }
    }

    private void decorateForBsp(ProjectViewNode node, PresentationData data) {
        if (node instanceof PsiDirectoryNode psiDirectoryNode) {
            removeJavaHackSourceRoot(data);
            setIconIfBazelDirectory(data, psiDirectoryNode);
        }
    }

    private void removeJavaHackSourceRoot(PresentationData receiver) {
        if (isJavaHackNode(receiver)) {
            clearPresentation(receiver);
        }
    }

    private boolean isJavaHackNode(PresentationData receiver) {
        String locationString = receiver.getLocationString();
        return locationString != null && locationString.contains("sources root");
    }

    private void clearPresentation(PresentationData receiver) {
        receiver.clearText();
        receiver.setLocationString("");
        receiver.setIcon(PlatformIconGroup.nodesFolder());
    }

    private void setIconIfBazelDirectory(PresentationData receiver, PsiDirectoryNode node) {
        VirtualFile virtualFile = node.getVirtualFile();
        if (virtualFile != null) {
            if (virtualFile instanceof NewVirtualFile newVirtualFile && anyConfigFile(newVirtualFile)) {
                receiver.setIcon(BazelPluginIcons.bazelDirectory());
            }
        }
    }

    // consulo: upstream virtualFile.iterInDbChildren().any { it.name in SUPPORTED_CONFIG_FILE_NAMES }
    private static boolean anyConfigFile(NewVirtualFile virtualFile) {
        for (VirtualFile it : virtualFile.iterInDbChildren()) {
            if (Arrays.asList(SUPPORTED_CONFIG_FILE_NAMES).contains(it.getName())) {
                return true;
            }
        }
        return false;
    }
}
