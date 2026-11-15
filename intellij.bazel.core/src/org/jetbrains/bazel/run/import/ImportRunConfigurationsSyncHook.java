// consulo: `import` is a Java keyword, the package gets a trailing underscore (the file keeps the upstream path)
package org.jetbrains.bazel.run.import_;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.jdom.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.run.BazelRunConfigurationState;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.run.config.BazelRunConfigurationType;
import org.jetbrains.bazel.run.state.Common;
import org.jetbrains.bazel.sync.ProjectSyncHook;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.config.project.getRootDir;

@ExtensionImpl
public class ImportRunConfigurationsSyncHook implements ProjectSyncHook {
    private static final String GOOGLE_BAZEL_RUN_CONFIG_TYPE = "BlazeCommandRunConfigurationType";

    private final Logger myLog = Logger.getInstance(ImportRunConfigurationsSyncHook.class);

    @Override
    public CompletableFuture<Void> onSync(ProjectSyncHook.ProjectSyncHookEnvironment environment) {
        // TODO consulo: move the subtask text to the BazelPluginLocalize YAML (upstream hard-codes it)
        return environment.withSubtask(LocalizeValue.localizeTODO("Import run configurations"), taskId -> {
            Project project = environment.project();
            ProjectView projectView = environment.server().getProjectView();

            RunManager runManager = RunManager.getInstance(project);
            boolean shouldSetSelectedConfiguration = runManager.getSelectedConfiguration() == null;
            Path workspaceRoot = getRootDir(environment.project()).toNioPath();
            List<RunnerAndConfigurationSettings> runConfigurations = new ArrayList<>();
            for (Path it : ProjectViewExtensions.getImportRunConfigurations(projectView)) {
                Path runConfigurationPath = workspaceRoot.resolve(it);
                try {
                    RunnerAndConfigurationSettings settings = importRunConfiguration(project, runConfigurationPath);
                    if (settings != null) {
                        runConfigurations.add(settings);
                    }
                }
                catch (Exception e) {
                    myLog.warn("Could not import " + runConfigurationPath, e);
                }
            }

            if (shouldSetSelectedConfiguration) {
                runManager.setSelectedConfiguration(runConfigurations.isEmpty() ? null : runConfigurations.get(0));
            }
            return CompletableFuture.<Void>completedFuture(null);
        });
    }

    private @Nullable RunnerAndConfigurationSettings importRunConfiguration(Project project, Path runConfigurationPath) throws Exception {
        Element runConfigurationXml = getConfigurationElement(JDOMUtil.load(runConfigurationPath.toFile()));
        if (runConfigurationXml == null) {
            return null;
        }
        RunnerAndConfigurationSettings settings;
        if (Objects.requireNonNull(runConfigurationXml.getAttributeValue("type")).equals(GOOGLE_BAZEL_RUN_CONFIG_TYPE)) {
            settings = loadGoogleBazelRunConfiguration(project, runConfigurationXml);
        }
        else {
            settings = loadRunConfigurationXmlNormally(project, runConfigurationXml);
        }
        return settings;
    }

    private @Nullable Element getConfigurationElement(Element runConfigurationXml) {
        Element element = runConfigurationXml;
        if (element.getName().equals("component") && "ProjectRunConfigurationManager".equals(element.getAttributeValue("name"))) {
            List<Element> children = element.getChildren();
            if (children.isEmpty()) {
                return null;
            }
            element = children.get(0);
        }
        return element.getName().equals("configuration") ? element : null;
    }

    private RunnerAndConfigurationSettings loadGoogleBazelRunConfiguration(Project project, Element runConfigurationXml) {
        String name = Objects.requireNonNull(runConfigurationXml.getAttributeValue("name"));
        Element blazeSettings = Objects.requireNonNull(runConfigurationXml.getChild("blaze-settings"));
        String googleHandlerId = Objects.requireNonNull(blazeSettings.getAttributeValue("handler-id"));
        String bazelCommand = Objects.requireNonNull(blazeSettings.getAttributeValue("blaze-command"));
        Label target = Label.parse(Objects.requireNonNull(blazeSettings.getChild("blaze-target")).getText());
        Element userFlag = blazeSettings.getChild("blaze-user-flag");
        String additionalBazelParams = userFlag == null ? null : replaceProjectDir(userFlag.getText(), project);
        Element userExeFlag = blazeSettings.getChild("blaze-user-exe-flag");
        String programArguments = userExeFlag == null ? null : replaceProjectDir(userExeFlag.getText(), project);

        Map<String, String> envsMap = new LinkedHashMap<>();
        Element envState = blazeSettings.getChild("env_state");
        Element envs = envState == null ? null : envState.getChild("envs");
        if (envs != null) {
            for (Element env : envs.getChildren()) {
                String envName = Objects.requireNonNull(env.getAttributeValue("name"));
                String value = replaceProjectDir(Objects.requireNonNull(env.getAttributeValue("value")), project);
                envsMap.put(envName, value);
            }
        }

        ConfigurationFactory factory =
            ConfigurationTypeUtil.findConfigurationType(BazelRunConfigurationType.class).getConfigurationFactories()[0];
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(name, factory);
        BazelRunConfiguration configuration = (BazelRunConfiguration) settings.getConfiguration();
        List<Label> targets = List.of(target);
        RunHandlerProvider googleRunHandler = GooglePluginAwareRunHandlerProvider.getRunHandlerProvider(googleHandlerId, bazelCommand);
        RunHandlerProvider runHandler =
            googleRunHandler != null ? googleRunHandler : RunHandlerProvider.getRunHandlerProviderOrThrow(project, targets);
        configuration.updateTargets(targets, runHandler);
        BazelRunHandler handler = configuration.getHandler();
        BazelRunConfigurationState<?> state = handler == null ? null : handler.getState();
        if (state instanceof Common.HasBazelParams hasBazelParams) {
            hasBazelParams.setAdditionalBazelParams(additionalBazelParams);
        }
        if (state instanceof Common.HasProgramArguments hasProgramArguments) {
            hasProgramArguments.setProgramArguments(programArguments);
        }
        if (state instanceof Common.HasEnv hasEnv) {
            hasEnv.getEnv().set(EnvironmentVariablesData.create(envsMap, true));
        }

        runManager.addConfiguration(settings);
        return settings;
    }

    /**
     * todo remove
     * Google's plugin sets the $PROJECT_DIR$ to <workspace_root>/.ijwb, while we instead use plain <workspace_root>.
     * That means that even if we just copied Google's run configurations, loading them directly would still not be possible.
     * Example: Google's plugin serializes the path to MODULE.bazel as $PROJECT_DIR$/../MODULE.bazel instead of $PROJECT_DIR$/MODULE.bazel
     */
    private String replaceProjectDir(String receiver, Project project) {
        return receiver.replace("$PROJECT_DIR$/..", getRootDir(project).getPath());
    }

    private @Nullable RunnerAndConfigurationSettings loadRunConfigurationXmlNormally(Project project, Element runConfigurationXml) {
        // TODO consulo: RunManagerImpl.loadConfiguration(Element, boolean) is internal in Consulo (execution-impl), such run
        //  configurations are not imported
        // val runManager = RunManagerImpl.getInstanceImpl(project)
        // return runManager.loadConfiguration(runConfigurationXml, false)
        return null;
    }
}
