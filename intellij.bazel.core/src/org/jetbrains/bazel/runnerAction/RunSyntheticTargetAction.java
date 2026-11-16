package org.jetbrains.bazel.runnerAction;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunManager;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jetbrains.bazel.run.synthetic.GenerateSyntheticTargetRunTaskProvider;
import org.jetbrains.bazel.run.synthetic.SyntheticRunTargetTemplateGenerator;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bazel.ui.gutters.BazelRunLocation;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// consulo: upstream RunSyntheticTargetAction.kt declares only this (internal) producer, LazyRunConfigurationProducer is
// Consulo's RunConfigurationProducer
@ExtensionImpl
class SyntheticRunConfigurationProducer extends RunConfigurationProducer<BazelRunConfiguration> {
    public SyntheticRunConfigurationProducer() {
        // consulo: the factory is passed to the constructor, upstream overrides getConfigurationFactory()
        super(BazelRunConfigurationType.getBazelRunConfigurationFactory());
    }

    // consulo: RunConfigurationProducer has no isDumbAware() in Consulo
    // override fun isDumbAware(): Boolean = true

    @Override
    protected boolean setupConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context,
        SimpleReference<PsiElement> sourceElement
    ) {
        if (!BazelFeatureFlags.getSyntheticRunEnable()) {
            return false;
        }
        if (!(context.getLocation() instanceof BazelRunLocation location)) {
            return false;
        }
        BuildTargetInfo target = location.getTarget();
        // Binary/test targets already have "normal" run configurations produced from them
        if (target.kind().ruleType() != RuleType.LIBRARY) {
            return false;
        }

        PsiElement targetElement = location.getOriginalLocation().getPsiElement();
        if (targetElement instanceof PsiFileSystemItem) {
            return false;
        }
        SyntheticRunTargetTemplateGenerator templateGenerator =
            SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, targetElement.getLanguage());
        if (templateGenerator == null) {
            return false;
        }
        Label syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement);

        // this runner is inferred from the original target
        RunHandlerProvider originalTargetProvider = RunHandlerProvider.getRunHandlerProvider(configuration.getProject(), List.of(target));
        if (originalTargetProvider == null) {
            return false;
        }
        configuration.updateRunProvider(List.of(syntheticTargetId), originalTargetProvider);

        // consulo: upstream BeforeRunTaskProvider.getProvider(project, GENERATE_SYNTHETIC_PROVIDER_ID), Consulo finds the before run
        // fragment by its task class
        GenerateSyntheticTargetRunTaskProvider provider = (GenerateSyntheticTargetRunTaskProvider) Objects.requireNonNull(
            BeforeRunFragment.find(configuration.getProject(), GenerateSyntheticTargetRunTaskProvider.Task.class)
        );
        SyntheticRunTargetTemplateGenerator.Params params = templateGenerator.getSyntheticParams(target, targetElement);
        GenerateSyntheticTargetRunTaskProvider.Task task = provider.createTask(configuration);
        GenerateSyntheticTargetRunTaskProvider.TaskState taskState = task.getTaskState();
        taskState.target = target.id().toString();
        taskState.language = targetElement.getLanguage().getID();
        taskState.params = params.data();
        // consulo: Consulo keeps the before run tasks in RunManager (upstream configuration.beforeRunTasks)
        List<BeforeRunTask> beforeRunTasks = new ArrayList<>();
        beforeRunTasks.add(task);
        beforeRunTasks.addAll(RunManager.getInstance(configuration.getProject()).getBeforeRunTasks(configuration));
        configuration.setBeforeRunTasksFromHandler(beforeRunTasks);

        configuration.setName(
            BazelPluginLocalize.runconfigSynthetic(
                BazelRepoMappingUtils.toShortString(target.id(), configuration.getProject())
            ).get()
        );

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(
        BazelRunConfiguration configuration,
        ConfigurationContext context
    ) {
        if (!(context.getLocation() instanceof BazelRunLocation location)) {
            return false;
        }
        BuildTargetInfo target = location.getTarget();
        if (target.kind().ruleType() != RuleType.LIBRARY) {
            return false;
        }
        PsiElement targetElement = context.getPsiLocation();
        if (targetElement == null) {
            return false;
        }
        if (targetElement instanceof PsiFileSystemItem) {
            return false;
        }
        SyntheticRunTargetTemplateGenerator templateGenerator =
            SyntheticRunTargetTemplateGenerator.getTemplateGenerator(target, targetElement.getLanguage());
        if (templateGenerator == null) {
            return false;
        }
        Label syntheticTargetId = templateGenerator.getSyntheticTargetLabel(target, targetElement);

        if (!configuration.getTargets().equals(List.of(syntheticTargetId))) {
            return false;
        }
        // consulo: Consulo keeps the before run tasks in RunManager (upstream configuration.beforeRunTasks)
        for (BeforeRunTask it : RunManager.getInstance(configuration.getProject()).getBeforeRunTasks(configuration)) {
            if (it instanceof GenerateSyntheticTargetRunTaskProvider.SyntheticRunTaskMarker) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPreferredConfiguration(ConfigurationFromContext self, ConfigurationFromContext other) {
        return !other.isProducedBy(BazelRunConfigurationProducer.class);
    }
}
