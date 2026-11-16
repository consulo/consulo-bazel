package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.SimpleCoverageAnnotator;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jspecify.annotations.Nullable;

import java.io.File;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class BazelCoverageAnnotator extends SimpleCoverageAnnotator {
    // consulo: upstream private top-level const of BazelCoverageAnnotator.kt
    private static final int AVERAGE_LINE_COUNT_IN_FILE = 100;

    @Inject
    public BazelCoverageAnnotator(Project project) {
        super(project);
    }

    public static BazelCoverageAnnotator getInstance(Project project) {
        return project.getInstance(BazelCoverageAnnotator.class);
    }

    // TODO consulo: SimpleCoverageAnnotator has no getRoots(project, dataManager, suite) hook (upstream override), Consulo always
    //  annotates the content roots of all modules instead of these roots; nothing calls this method
    public VirtualFile[] getRoots(
        Project project,
        CoverageDataManager dataManager,
        CoverageSuitesBundle suite
    ) {
        return BazelCoverageSuite.getRoots(suite).toArray(VirtualFile.EMPTY_ARRAY);
    }

    @Override
    protected @Nullable FileCoverageInfo fillInfoForUncoveredFile(File file) {
        // Only include Java/Kotlin/etc. files in the total count
        if (LanguageClass.fromExtension(FileUtil.getExtension(file.getName())) == null) {
            return null;
        }

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath());
        if (virtualFile == null) {
            return null;
        }
        if (ReadAction.compute(() ->
            ProjectFileIndex.getInstance(getProject()).isInTestSourceContent(virtualFile) ||
                !ProjectFileIndex.getInstance(getProject()).isInSource(virtualFile)
        )) {
            return null;
        }

        FileCoverageInfo info = new FileCoverageInfo();
        info.coveredLineCount = 0;
        // Best-effort attempt without actually reading the file from disk (slow!).
        // Even if we were to read it and count the lines, the number of statements is not the same as the number of physical lines.
        info.totalLineCount = AVERAGE_LINE_COUNT_IN_FILE;
        return info;
    }
}
