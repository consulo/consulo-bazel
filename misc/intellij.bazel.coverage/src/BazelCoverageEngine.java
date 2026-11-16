package org.jetbrains.bazel.run.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.view.CoverageViewExtension;
import consulo.execution.test.AbstractTestProxy;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl(id = "bazelCoverageEngine")
public class BazelCoverageEngine extends CoverageEngine {
    public static BazelCoverageEngine getInstance() {
        return Application.get().getExtensionPoint(CoverageEngine.class).findExtensionOrFail(BazelCoverageEngine.class);
    }

    @Override
    public String getPresentableText() {
        return BazelPluginLocalize.coverageBazelPresentableText().get();
    }

    @Override
    public boolean isApplicableTo(@Nullable RunConfigurationBase configuration) {
        if (!(configuration instanceof BazelRunConfiguration bazelRunConfiguration)) {
            return false;
        }
        BazelRunHandler handler = bazelRunConfiguration.getHandler();
        return handler != null && handler.isTestHandler();
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public boolean canHavePerTestCoverage(@Nullable RunConfigurationBase conf) {
        return false;
    }

    @Override
    public CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nullable RunConfigurationBase configuration) {
        return new BazelCoverageEnabledConfiguration(Objects.requireNonNull(configuration));
    }

    // consulo: upstream also overrides createCoverageSuite(config: CoverageEnabledConfiguration), which Consulo does not have;
    //  Consulo's CoverageDataManager calls createCoverageSuite(runner, name, fileProvider, config) below instead

    @Override
    public @Nullable CoverageSuite createCoverageSuite(
        CoverageRunner runner,
        String name,
        CoverageFileProvider fileProvider,
        @Nullable String[] filters,
        long timestamp,
        @Nullable String suiteToMerge,
        boolean coverageByTestEnabled,
        boolean branchCoverage,
        boolean trackTestFolders,
        @Nullable Project project
    ) {
        if (project == null) {
            return null;
        }
        return new BazelCoverageSuite(name, project, runner, fileProvider, timestamp);
    }

    // consulo: upstream uses config.createFileProvider() and config.createTimestamp(), which Consulo's CoverageEnabledConfiguration
    //  does not have: the file provider is passed in, the timestamp is the current time
    @Override
    public CoverageSuite createCoverageSuite(
        CoverageRunner runner,
        String name,
        CoverageFileProvider coverageDataFileProvider,
        CoverageEnabledConfiguration config
    ) {
        return new BazelCoverageSuite(
            name,
            config.getConfiguration().getProject(),
            runner,
            coverageDataFileProvider,
            System.currentTimeMillis()
        );
    }

    @Override
    public CoverageSuite createEmptyCoverageSuite(CoverageRunner coverageRunner) {
        return new BazelCoverageSuite();
    }

    @Override
    public CoverageAnnotator getCoverageAnnotator(Project project) {
        return BazelCoverageAnnotator.getInstance(project);
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public boolean coverageEditorHighlightingApplicableTo(PsiFile psiFile) {
        return true;
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public boolean acceptedByFilters(PsiFile psiFile, CoverageSuitesBundle suite) {
        return true;
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public boolean recompileProjectAndRerunAction(Module module, CoverageSuitesBundle suite, Runnable chooseSuiteAction) {
        return false;
    }

    @Override
    public String getQualifiedName(File outputFile, PsiFile sourceFile) {
        return BazelCoverageRunner.getNameInCoverageData(Objects.requireNonNull(sourceFile.getVirtualFile()));
    }

    // consulo: abstract in Consulo, IntelliJ's default (the qualified name of the source file)
    @Override
    public Set<String> getQualifiedNames(PsiFile sourceFile) {
        VirtualFile virtualFile = sourceFile.getVirtualFile();
        if (virtualFile == null) {
            return Set.of();
        }
        String qName = getQualifiedName(VirtualFileUtil.virtualToIoFile(virtualFile), sourceFile);
        return Set.of(qName);
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public List<PsiElement> findTestsByNames(String[] testNames, Project project) {
        return List.of();
    }

    // consulo: abstract in Consulo, IntelliJ's default
    @Override
    public @Nullable String getTestMethodName(PsiElement element, AbstractTestProxy testProxy) {
        return null;
    }

    @Override
    public boolean coverageProjectViewStatisticsApplicableTo(VirtualFile fileOrDir) {
        return true;
    }

    // consulo: the Consulo createCoverageViewExtension() also takes the coverage view state
    @Override
    public CoverageViewExtension createCoverageViewExtension(
        Project project,
        CoverageSuitesBundle suiteBundle,
        CoverageViewManager.StateBean stateBean
    ) {
        return new BazelCoverageViewExtension(project, suiteBundle.getAnnotator(project), suiteBundle, stateBean);
    }
}
