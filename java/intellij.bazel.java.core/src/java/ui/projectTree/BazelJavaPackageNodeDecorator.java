package org.jetbrains.bazel.java.ui.projectTree;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.project.ui.view.tree.TreeViewUtil;
import consulo.ui.ex.tree.PresentationData;
import jakarta.inject.Inject;
import org.jetbrains.bazel.ui.projectTree.BazelProjectViewDirectoryHelper;

@ExtensionImpl(order = "first")
public class BazelJavaPackageNodeDecorator implements ProjectViewNodeDecorator {
    private final Project myProject;

    @Inject
    public BazelJavaPackageNodeDecorator(Project project) {
        myProject = project;
    }

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        if (org.jetbrains.bazel.config.project.isBazelProject(myProject)
            && node instanceof PsiDirectoryNode directoryNode
            && directoryNode.getSettings().isFlattenPackages()) {
            PsiDirectory directory = directoryNode.getValue();
            if (directory == null) {
                return;
            }
            PsiDirectory parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null) {
                return;
            }
            BazelProjectViewDirectoryHelper helper = new BazelProjectViewDirectoryHelper(myProject);
            if (helper.getPackageName(parentDirectory) == null) {
                return;
            }

            String packageName = helper.getPackageName(directory);
            if (packageName == null) {
                return;
            }
            if (packageName.isEmpty()) {
                return;
            }

            data.clearText();
            PsiJavaPackage psiPackage =
                directoryNode.getSettings().isAbbreviatePackageNames()
                    ? JavaPsiFacade.getInstance(myProject).findPackage(packageName)
                    : null;
            data.setPresentableText(psiPackage != null ? TreeViewUtil.calcAbbreviatedPackageFQName(psiPackage) : packageName);
        }
    }
}
