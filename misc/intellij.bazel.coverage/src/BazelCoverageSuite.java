package org.jetbrains.bazel.run.coverage;

import consulo.execution.coverage.BaseCoverageSuite;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.data.CoverageLine;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageUnit;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BazelCoverageSuite extends BaseCoverageSuite {
    private @Nullable VirtualFile myRootDirectory = null;

    public @Nullable VirtualFile getRootDirectory() {
        return myRootDirectory;
    }

    public BazelCoverageSuite() {
        super();
    }

    // consulo: the Consulo BaseCoverageSuite constructor takes the per-test/tracing/track-test-folders flags explicitly
    //  (false, as in IntelliJ's BaseCoverageSuite(name, project, runner, fileProvider, timestamp))
    public BazelCoverageSuite(
        String name,
        Project project,
        CoverageRunner runner,
        CoverageFileProvider fileProvider,
        long timestamp
    ) {
        super(name, fileProvider, timestamp, false, false, false, runner, project);
    }

    @Override
    public CoverageEngine getCoverageEngine() {
        return BazelCoverageEngine.getInstance();
    }

    @Override
    public void setCoverageData(@Nullable CoverageProjectData projectData) {
        super.setCoverageData(projectData);
        if (projectData == null) {
            myRootDirectory = null;
            return;
        }
        List<VirtualFile> files = new ArrayList<>();
        for (CoverageUnit classData : projectData.getUnits()) {
            boolean covered = false;
            for (@Nullable CoverageLine line : classData.getLines()) {
                // consulo: Consulo lines are indexed by line number, entries without code are null
                if (line != null && line.getHits() > 0) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                continue;
            }
            VirtualFile file = BazelCoverageRunner.getFileByCoverageDataName(classData.getName());
            // consulo: files that cannot be found any more are skipped (upstream passes them to getCommonAncestor)
            if (file != null) {
                files.add(file);
            }
        }
        VirtualFile commonAncestor = VirtualFileUtil.getCommonAncestor(files);
        if (commonAncestor == null) {
            myRootDirectory = null;
        }
        else {
            myRootDirectory = commonAncestor.isDirectory() ? commonAncestor : commonAncestor.getParent();
        }
    }

    // consulo: upstream companion object member
    public static List<VirtualFile> getRoots(CoverageSuitesBundle bundle) {
        Set<VirtualFile> roots = new LinkedHashSet<>();
        for (CoverageSuite suite : bundle.getSuites()) {
            if (suite instanceof BazelCoverageSuite bazelCoverageSuite) {
                VirtualFile rootDirectory = bazelCoverageSuite.getRootDirectory();
                if (rootDirectory != null) {
                    roots.add(rootDirectory);
                }
            }
        }
        if (roots.isEmpty()) {
            return List.of();
        }

        // consulo: CoverageSuitesBundle has no getProject(), IntelliJ returns the project of the first suite
        VirtualFile projectRoot = org.jetbrains.bazel.config.project.getRootDir(bundle.getSuites()[0].getProject());
        if (roots.contains(projectRoot)) {
            return List.of(projectRoot);
        }

        List<VirtualFile> rootsThatDontContainEachOther = new ArrayList<>();
        for (VirtualFile root : roots) {
            VirtualFile parent = root.getParent();
            if (parent == null || !VirtualFileUtil.isUnder(parent, roots)) {
                rootsThatDontContainEachOther.add(root);
            }
        }
        return rootsThatDontContainEachOther;
    }
}
