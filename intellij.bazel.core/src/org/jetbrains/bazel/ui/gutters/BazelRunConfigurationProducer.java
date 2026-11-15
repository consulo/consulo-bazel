package org.jetbrains.bazel.ui.gutters;

import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jetbrains.bazel.run.state.Common.HasEnv;
import org.jetbrains.bazel.run.state.Common.HasProgramArguments;
import org.jetbrains.bazel.run.state.Test.HasTestFilter;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// consulo: LazyRunConfigurationProducer -> RunConfigurationProducer (the factory is passed to the constructor);
//   upstream isDumbAware() = true -> DumbAware
public abstract class BazelRunConfigurationProducer extends RunConfigurationProducer<BazelRunConfiguration> implements DumbAware {
    protected BazelRunConfigurationProducer() {
        super(BazelRunConfigurationType.getBazelRunConfigurationFactory());
    }

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
        if (!(context.getLocation() instanceof BazelRunLocation location)) {
            return false;
        }
        BuildTarget target = location.getTarget();
        if (!acceptsTarget(target)) {
            return false;
        }
        PsiElement element = location.getOriginalLocation().getPsiElement();
        GutterAction gutterAction = getGutterAction(element, target);
        if (gutterAction == null) {
            return false;
        }
        configuration.updateTargets(List.of(BuildTarget.getId(target)), RunHandlerProvider.getRunHandlerProvider(context.getProject(), List.of(target)));
        BazelRunHandler handler = configuration.getHandler();
        if (handler == null) {
            return false;
        }
        gutterAction.applyTo(handler.getState());
        RunConfigurationExtensionsManager<? super RunConfigurationBase, ?> extensionsManager = handler.getExtensionsManager();
        if (extensionsManager != null) {
            extensionsManager.extendCreatedConfiguration(configuration, location);
        }
        String targetName = BazelRepoMappingUtils.toShortString(BuildTarget.getId(target), context.getProject());
        String additionalLocationString = gutterAction.getAdditionalLocationString();
        configuration.setName(
            additionalLocationString != null
                ? BazelPluginLocalize.runconfigNameAndLocation(
                    targetName,
                    additionalLocationString
                ).get()
                : targetName
        );
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(BazelRunConfiguration configuration, ConfigurationContext context) {
        if (!(context.getLocation() instanceof BazelRunLocation location)) {
            return false;
        }
        BuildTarget target = location.getTarget();
        if (!acceptsTarget(target)) {
            return false;
        }
        if (!configuration.getTargets().equals(List.of(BuildTarget.getId(target)))) {
            return false;
        }
        PsiElement element = location.getOriginalLocation().getPsiElement();
        GutterAction gutterAction = getGutterAction(element, target);
        if (gutterAction == null) {
            return false;
        }
        BazelRunHandler handler = configuration.getHandler();
        return handler != null && gutterAction.isFromContext(handler.getState());
    }

    public abstract @Nullable GutterAction getGutterAction(
        PsiElement element,
        BuildTarget target
    );

    protected boolean acceptsTarget(BuildTarget target) {
        return target.getKind().isExecutable();
    }

    public static class GutterAction {
        private final @Nullable String myTestFilter;
        private final List<String> myProgramArguments;
        private final Map<String, String> myEnv;
        private final @Nullable String myAdditionalLocationString;

        // consulo: overload for the upstream default arguments
        public GutterAction() {
            this(null, List.of(), Map.of(), null);
        }

        public GutterAction(
            @Nullable String testFilter,
            List<String> programArguments,
            Map<String, String> env,
            @Nullable String additionalLocationString
        ) {
            myTestFilter = testFilter;
            myProgramArguments = programArguments;
            myEnv = env;
            myAdditionalLocationString = additionalLocationString;
        }

        public @Nullable String getTestFilter() {
            return myTestFilter;
        }

        public List<String> getProgramArguments() {
            return myProgramArguments;
        }

        public Map<String, String> getEnv() {
            return myEnv;
        }

        public @Nullable String getAdditionalLocationString() {
            return myAdditionalLocationString;
        }

        public void applyTo(BazelRunConfigurationState<?> state) {
            if (state instanceof HasTestFilter hasTestFilter) {
                hasTestFilter.setTestFilter(myTestFilter);
            }
            if (state instanceof HasProgramArguments hasProgramArguments) {
                hasProgramArguments.setProgramArguments(formatProgramArguments(myProgramArguments));
            }
            if (state instanceof HasEnv hasEnv) {
                hasEnv.getEnv().getEnvs().putAll(myEnv);
            }
        }

        public boolean isFromContext(BazelRunConfigurationState<?> state) {
            if (state instanceof HasTestFilter it) {
                if (!Objects.equals(myTestFilter, it.getTestFilter())) {
                    return false;
                }
            }
            if (state instanceof HasProgramArguments it) {
                if (!Objects.equals(formatProgramArguments(myProgramArguments), it.getProgramArguments())) {
                    return false;
                }
            }
            if (state instanceof HasEnv hasEnv) {
                Map<String, String> envs = hasEnv.getEnv().getEnvs();
                Map<String, String> merged = new HashMap<>(envs);
                merged.putAll(myEnv);
                if (!merged.equals(envs)) {
                    return false;
                }
            }
            return true;
        }

        private static @Nullable String formatProgramArguments(List<String> arguments) {
            String result = arguments.stream().map(argument -> {
                String escaped = argument.replace("\"", "\\\"");
                return "\"" + escaped + "\"";
            }).collect(Collectors.joining(" "));
            return result.isEmpty() ? null : result;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            GutterAction that = (GutterAction) other;

            if (!Objects.equals(myTestFilter, that.myTestFilter)) {
                return false;
            }
            if (!myProgramArguments.equals(that.myProgramArguments)) {
                return false;
            }
            if (!myEnv.equals(that.myEnv)) {
                return false;
            }
            if (!Objects.equals(myAdditionalLocationString, that.myAdditionalLocationString)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(myTestFilter);
            result = 31 * result + myProgramArguments.hashCode();
            result = 31 * result + myEnv.hashCode();
            result = 31 * result + Objects.hashCode(myAdditionalLocationString);
            return result;
        }

        // consulo: upstream copy() has default arguments, Java callers pass every value
        public GutterAction copy(
            @Nullable String testFilter,
            List<String> programArguments,
            Map<String, String> env,
            @Nullable String additionalLocationString
        ) {
            return new GutterAction(testFilter, programArguments, env, additionalLocationString);
        }
    }
}
