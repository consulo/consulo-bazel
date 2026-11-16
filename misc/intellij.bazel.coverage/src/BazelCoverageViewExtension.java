package org.jetbrains.bazel.run.coverage;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.view.CoverageListNode;
import consulo.execution.coverage.view.CoverageListRootNode;
import consulo.execution.coverage.view.DirectoryCoverageViewExtension;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

// consulo: the Consulo DirectoryCoverageViewExtension and coverage nodes also take the coverage view state
public class BazelCoverageViewExtension extends DirectoryCoverageViewExtension {
    private final List<VirtualFile> myRoots;

    public BazelCoverageViewExtension(
        Project project,
        CoverageAnnotator coverageAnnotator,
        CoverageSuitesBundle bundle,
        CoverageViewManager.StateBean stateBean
    ) {
        super(project, coverageAnnotator, bundle, stateBean);
        myRoots = BazelCoverageSuite.getRoots(bundle);
    }

    @Override
    @RequiredReadAction
    public AbstractTreeNode createRootNode() {
        if (myRoots.size() == 1) {
            return new CoverageListRootNode(myProject, toPsiDirectory(myRoots.get(0)), getSuitesBundle(), getStateBean()) {
                @Override
                @RequiredReadAction
                public Collection<? extends AbstractTreeNode> getChildren() {
                    return getChildrenNodes(this);
                }
            };
        }
        else {
            return new CoverageListRootNode(
                myProject,
                toPsiDirectory(org.jetbrains.bazel.config.project.getRootDir(myProject)),
                getSuitesBundle(),
                getStateBean()
            );
        }
    }

    @Override
    public List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node) {
        if (node instanceof CoverageListRootNode && myRoots.size() != 1) {
            List<AbstractTreeNode> result = new ArrayList<>();
            for (VirtualFile root : myRoots) {
                result.add(new CoverageListNode(myProject, toPsiDirectory(root), getSuitesBundle(), getStateBean()));
            }
            return result;
        }
        return super.getChildrenNodes(node);
    }

    // consulo: upstream private extension fun VirtualFile.toPsiDirectory()
    private PsiDirectory toPsiDirectory(VirtualFile receiver) {
        VirtualFile directory = !receiver.isDirectory() ? receiver.getParent() : receiver;
        return Objects.requireNonNull(PsiManager.getInstance(myProject).findDirectory(Objects.requireNonNull(directory)));
    }
}
