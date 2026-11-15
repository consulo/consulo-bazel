package org.jetbrains.bazel.ui.projectTree.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.localize.ExecutionLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils.calculateWildcardLabel;

@ExtensionImpl
public class BazelAllTestsInDirectoryRunConfigurationProducer extends RunConfigurationProducer<BazelRunConfiguration> {
    public BazelAllTestsInDirectoryRunConfigurationProducer() {
        super(BazelRunConfigurationType.getBazelRunConfigurationFactory());
    }

    // consulo: RunConfigurationProducer has no isDumbAware() (upstream returns true)

    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return BazelRunConfigurationType.getBazelRunConfigurationFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context,
        SimpleReference<PsiElement> sourceElement
    ) {
        Project project = context.getProject();
        if (!isProjectApplicable(project)) {
            return false;
        }
        VirtualFile selectedDirectory = getSelectedDirectory(context);
        if (selectedDirectory == null) {
            return false;
        }
        if (ProjectFileIndex.getInstance(project).isExcluded(selectedDirectory)) {
            return false;
        }

        Label.ResolvedLabel testTarget = calculateWildcardLabel(context.getProject(), selectedDirectory);
        if (testTarget == null) {
            return false;
        }
        configuration.updateTargets(List.of(testTarget), null);
        configuration.setName(ExecutionLocalize.testInScopePresentableText(selectedDirectory.getName()).get());
        return true;
    }

    public boolean isProjectApplicable(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    @Override
    public boolean isConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context
    ) {
        VirtualFile selectedDirectory = getSelectedDirectory(context);
        if (selectedDirectory == null) {
            return false;
        }
        Label.ResolvedLabel testTarget = calculateWildcardLabel(context.getProject(), selectedDirectory);
        if (testTarget == null) {
            return false;
        }
        if (!configuration.getTargets().equals(List.of(testTarget))) {
            return false;
        }
        return configuration.getName().startsWith(ExecutionLocalize.testInScopePresentableText(selectedDirectory.getName()).get());
    }

    private @Nullable VirtualFile getSelectedDirectory(ConfigurationContext context) {
        return context.getPsiLocation() instanceof PsiDirectory psiDirectory ? psiDirectory.getVirtualFile() : null;
    }
}
