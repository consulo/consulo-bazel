package org.jetbrains.bazel.run.config;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.execution.ui.SettingsEditorRunConfigurationFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunManager;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.sm.runner.SMRunnerConsolePropertiesProvider;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Use BazelRunConfigurationType.createTemplateConfiguration(project) to create a new BazelRunConfiguration.
// TODO consulo: RunConfigurationWithSuppressedDefaultDebugAction is missing in Consulo
// consulo: FusAwareRunConfiguration (usage statistics) is not ported
public class BazelRunConfiguration extends LocatableConfigurationBase implements SMRunnerConsolePropertiesProvider {
    private static final String TARGET_TAG = "bsp-target";
    private static final String BSP_STATE_TAG = "bsp-state";
    private static final String HANDLER_STATE_TAG = "handler-state";
    private static final String HANDLER_PROVIDER_ATTR = "handler-provider-id";
    private static final String FORCED_HANDLER_PROVIDER_ATTR = "forced-handler-provider-id";
    private static final String CHECK_VISIBILITY_ATTR = "check-visibility";

    private static final Logger LOG = Logger.getInstance(BazelRunConfiguration.class);

    // Used in BazelRerunFailedTestsAction
    public static final Key<BazelRunConfiguration> BAZEL_RUN_CONFIGURATION_KEY = Key.create("BAZEL_RUN_CONFIGURATION_KEY");

    // private because we need to set the targets directly when running readExternal
    private List<Label> myTargets = List.of();

    private boolean myDoVisibilityCheck = true;

    /**
     * Run handler manually chosen by the user in the run config editor
     */
    private @Nullable String myForcedHandlerProviderId;

    private @Nullable RunHandlerProvider myHandlerProvider;

    private @Nullable BazelRunHandler myHandler;

    public BazelRunConfiguration(Project project, String name, BazelRunConfigurationType configurationType) {
        super(project, configurationType, name);
    }

    public List<Label> getTargets() {
        return myTargets;
    }

    public boolean getDoVisibilityCheck() {
        return myDoVisibilityCheck;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        try {
            checkConfigurationUnsafe();
        }
        catch (RuntimeConfigurationError e) {
            // rethrow configuration error
            throw e;
        }
        catch (Throwable e) {
            // exception here can prevent project from loading
            LOG.error("Error checking configuration", e);
        }
    }

    public void checkConfigurationUnsafe() throws RuntimeConfigurationError {
        TargetStorage utils = TargetStorage.getTargetStorage(getProject());
        List<BuildTargetInfo> selectedTargets = new ArrayList<>();
        for (Label it : myTargets) {
            BuildTargetInfo target = TargetStorage.getTargetInfo(utils, it);
            if (target == null) {
                return; // skip validations when any target is missing
            }
            if (!target.kind().isExecutable()) {
                throw new RuntimeConfigurationError(BazelPluginLocalize.runconfigBazelErrorsTargetNotExecutable(it));
            }
            selectedTargets.add(target);
        }
        if (selectedTargets.isEmpty()) {
            throw new RuntimeConfigurationError(BazelPluginLocalize.runconfigBazelErrorsNoTargets());
        }
        if (findForcedHandlerProvider() != null) {
            return;
        }
        Set<RunHandlerProvider> providers = new LinkedHashSet<>();
        for (BuildTargetInfo target : selectedTargets) {
            RunHandlerProvider provider = RunHandlerProvider.getRunHandlerProvider(getProject(), List.of(target));
            if (provider == null) {
                throw new RuntimeConfigurationError(BazelPluginLocalize.runconfigBazelErrorsTargetNotSupported(target.id()));
            }
            providers.add(provider);
        }
        if (providers.size() > 1) {
            throw new RuntimeConfigurationError(
                BazelPluginLocalize.runconfigBazelErrorsMultiplePlatformTargets(
                    providers.stream().map(RunHandlerProvider::getId).collect(Collectors.joining(", "))
                )
            );
        }
    }

    public void updateTargets(List<Label> newTargets) {
        updateTargets(newTargets, null);
    }

    public void updateTargets(List<Label> newTargets, @Nullable RunHandlerProvider runHandlerProvider) {
        if (newTargets.equals(myTargets)) {
            return;
        }
        myTargets = newTargets;
        if (newTargets.isEmpty()) {
            return;
        }
        // `updateTargets` is called by the editor on each change. It must not fail, because it will spam with errors while editing
        // `RunHandlerProvider.getRunHandlerProviderOrNull` is used as a workaround
        RunHandlerProvider provider = runHandlerProvider;
        if (provider == null) {
            provider = findForcedHandlerProvider();
        }
        if (provider == null) {
            provider = RunHandlerProvider.getRunHandlerProviderOrNull(getProject(), newTargets);
        }
        if (provider == null) {
            return;
        }
        updateHandlerIfDifferentProvider(provider);
    }

    /**
     * Run handler manually chosen by the user in the run config editor
     */
    public @Nullable String getForcedHandlerProviderId() {
        return myForcedHandlerProviderId;
    }

    public void updateForcedHandlerProvider(@Nullable String providerId) {
        if (Objects.equals(providerId, myForcedHandlerProviderId)) {
            return;
        }
        myForcedHandlerProviderId = providerId;
        RunHandlerProvider provider = findForcedHandlerProvider();
        if (provider == null) {
            provider = RunHandlerProvider.getRunHandlerProviderOrNull(getProject(), myTargets);
        }
        if (provider == null) {
            return;
        }
        updateHandlerIfDifferentProvider(provider);
    }

    private @Nullable RunHandlerProvider findForcedHandlerProvider() {
        String forcedHandlerProviderId = myForcedHandlerProviderId;
        return forcedHandlerProviderId != null ? RunHandlerProvider.findRunHandlerProvider(forcedHandlerProviderId) : null;
    }

    public void updateRunProvider(List<Label> newTargets, RunHandlerProvider runHandlerProvider) {
        myTargets = newTargets;
        updateHandler(runHandlerProvider);
    }

    public void disableVisibilityCheck() {
        myDoVisibilityCheck = false;
    }

    public @Nullable RunHandlerProvider getHandlerProvider() {
        return myHandlerProvider;
    }

    public @Nullable BazelRunHandler getHandler() {
        return myHandler;
    }

    private void updateHandlerIfDifferentProvider(RunHandlerProvider newProvider) {
        RunHandlerProvider handlerProvider = myHandlerProvider;
        if (handlerProvider != null && newProvider.getClass() == handlerProvider.getClass()) {
            return;
        }
        updateHandler(newProvider);
    }

    private void updateHandler(RunHandlerProvider newProvider) {
        Element bspBeforeChange = createBspElement();
        myHandlerProvider = newProvider;
        BazelRunHandler newRunHandler = newProvider.createRunHandler(this);
        myHandler = newRunHandler;
        if (bspBeforeChange != null) {
            Element oldHandlerState = bspBeforeChange.getChild(HANDLER_STATE_TAG);
            if (oldHandlerState == null) {
                return;
            }
            try {
                newRunHandler.getState().readExternal(oldHandlerState);
            }
            catch (Exception e) {
                LOG.error("Failed to read BSP state", e);
            }
        }
    }

    @Override
    public RunConfiguration clone() {
        BazelRunConfiguration result = (BazelRunConfiguration) super.clone();
        // Deep-clone the handler to allow changing handler.state without changing the original configuration
        RunHandlerProvider handlerProvider = myHandlerProvider;
        if (handlerProvider != null) {
            result.updateHandler(handlerProvider);
        }
        return result;
    }

    // consulo: RunConfiguration has no getConfigurationEditor(), the editor is shown through createFragments()
    public SettingsEditor<BazelRunConfiguration> getConfigurationEditor() {
        return new BazelRunConfigurationDynamicEditor(this);
    }

    // consulo: the whole upstream editor (getConfigurationEditor()) is one Consulo run configuration fragment
    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(
            new SettingsEditorRunConfigurationFragment<BazelRunConfiguration>(
                "bazel.run.configuration",
                LocalizeValue.of(BazelPluginConstants.BAZEL_DISPLAY_NAME),
                this::getConfigurationEditor
            )
        );
    }

    @Override
    public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
        BazelRunHandler handler = myHandler;
        return handler == null ? null : handler.getRunProfileState(executor, environment);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        // FUCKING CLONE
        Element bspStateElement = element.getChild(BSP_STATE_TAG);
        if (bspStateElement == null) {
            return;
        }
        Element bspElement = bspStateElement.clone();

        List<String> targets = new ArrayList<>();
        for (Element targetElement : bspElement.getChildren(TARGET_TAG)) {
            targets.add(targetElement.getText());
        }

        myTargets = targets.stream().map(Label::parse).toList();
        myForcedHandlerProviderId = bspElement.getAttributeValue(FORCED_HANDLER_PROVIDER_ATTR);

        // It should be possible to load the configuration before the project is synchronized,
        // so we can't access targets' data here. Instead, we have to use the stored provider ID.
        // TODO: is that true?
        String providerId = bspElement.getAttributeValue(HANDLER_PROVIDER_ATTR);
        if (providerId == null) {
            LOG.warn("No handler provider ID found in run configuration");
            return;
        }

        RunHandlerProvider provider = RunHandlerProvider.findRunHandlerProvider(providerId);
        if (provider == null) {
            LOG.warn("Failed to find run handler provider with ID " + providerId);
            provider = RunHandlerProvider.getRunHandlerProviderOrNull(getProject(), myTargets);
            if (provider == null) {
                return;
            }
        }
        updateHandlerIfDifferentProvider(provider);
        // TODO the above already reads
        Element handlerStateElement = bspElement.getChild(HANDLER_STATE_TAG);
        if (handlerStateElement == null) {
            return;
        }
        try {
            BazelRunHandler handler = myHandler;
            if (handler != null) {
                handler.getState().readExternal(handlerStateElement);
            }
        }
        catch (Exception e) {
            LOG.error("Failed to read BSP state", e);
        }

        String checkVisibility = bspElement.getAttributeValue(CHECK_VISIBILITY_ATTR);
        myDoVisibilityCheck = checkVisibility == null || Boolean.parseBoolean(checkVisibility);

        // consulo: RunConfigurationExtensionsManager has no readExternal(), Consulo keeps the extension settings as fragment states
        // handler?.extensionsManager?.readExternal(this, element)
    }

    // TODO: ideally we'd use an existing serialization mechanism like https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
    //  but it's hard to figure out how to implement it in our case, so for now let's use the franken-implementation
    //  inspired by Google's plugin (which probably predates modern IJ state serialization)
    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element bspState = createBspElement();
        if (bspState == null) {
            return;
        }
        element.removeChildren(BSP_STATE_TAG);
        element.addContent(bspState);
        // consulo: RunConfigurationExtensionsManager has no writeExternal(), Consulo keeps the extension settings as fragment states
        // handler?.extensionsManager?.writeExternal(this, element)
    }

    /**
     * HACK
     * This method is called during deserialization of run tasks which happens AFTER the {@link BazelRunHandler}s are deserialized.
     * So if the Bazel plugin uses a new logic for creating before run tasks in a newer version inside one of the handlers,
     * it will be overruled by this deserialization unless we forcefully disable this.
     *
     * @see #setBeforeRunTasksFromHandler
     */
    // consulo: Consulo keeps the before run tasks in RunManager, a run configuration cannot filter them while they are loaded
    // override fun setBeforeRunTasks(value: List<BeforeRunTask<*>>) {
    //   val tasks = value.filter { it is SyntheticRunTaskMarker }
    //   if (tasks.isNotEmpty()) {
    //     super.setBeforeRunTasks(tasks)
    //   }
    // }

    public void setBeforeRunTasksFromHandler(List<BeforeRunTask> value) {
        // consulo: upstream calls super.setBeforeRunTasks(value), Consulo keeps the before run tasks in RunManager.
        //   Producers create configurations while executor actions update in parallel in the background, and RunManagerImpl's
        //   before run task map is not thread-safe there (fixed in the platform by a concurrent map), so the writes are serialized
        RunManager runManager = RunManager.getInstance(getProject());
        synchronized (runManager) {
            runManager.setBeforeRunTasks(this, value, false);
        }
    }

    @Override
    public SMTRunnerConsoleProperties createTestConsoleProperties(Executor executor) {
        return new BazelTestConsoleProperties(this, executor);
    }

    @Override
    public String suggestedName() {
        return myTargets.stream().map(Label::toString).collect(Collectors.joining(" "));
    }

    // consulo: getAdditionalUsageData() (FusAwareRunConfiguration) is dropped, usage statistics are not ported

    private @Nullable Element createBspElement() {
        RunHandlerProvider provider = myHandlerProvider;
        if (provider == null) {
            return null;
        }
        BazelRunHandler handler = myHandler;
        if (handler == null) {
            return null;
        }
        Element element = new Element(BSP_STATE_TAG);
        for (Label target : myTargets) {
            Element targetElement = new Element(TARGET_TAG);
            targetElement.setText(target.toString());
            element.addContent(targetElement);
        }
        element.setAttribute(HANDLER_PROVIDER_ATTR, provider.getId());
        String forcedHandlerProviderId = myForcedHandlerProviderId;
        if (forcedHandlerProviderId != null) {
            element.setAttribute(FORCED_HANDLER_PROVIDER_ATTR, forcedHandlerProviderId);
        }
        Element handlerState = new Element(HANDLER_STATE_TAG);
        handler.getState().writeExternal(handlerState);
        element.addContent(handlerState);

        element.setAttribute(CHECK_VISIBILITY_ATTR, String.valueOf(myDoVisibilityCheck));
        return element;
    }

    public static BazelRunConfiguration get(ExecutionEnvironment environment) {
        BazelRunConfiguration configuration = environment.getUserData(BAZEL_RUN_CONFIGURATION_KEY);
        return configuration != null ? configuration : (BazelRunConfiguration) environment.getRunProfile();
    }
}
