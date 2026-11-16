package org.jetbrains.bazel.run.synthetic;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Transient;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static org.jetbrains.bazel.config.project.getRootDir;

// consulo: IntelliJ BeforeRunTaskProvider is Consulo's BeforeRunFragment (stepsBeforeRunProvider -> project @ExtensionImpl)
@ExtensionImpl
public class GenerateSyntheticTargetRunTaskProvider implements BeforeRunFragment<GenerateSyntheticTargetRunTaskProvider.Task> {
    private static final String GENERATE_SYNTHETIC_PROVIDER_NAME = "BazelGenerateSyntheticTargetRunTaskProvider";

    // consulo: Consulo before run tasks are identified by their state class (BeforeRunFragment.find(project, Task.class)), the key is
    // kept for upstream callers
    public static final Key<Task> GENERATE_SYNTHETIC_PROVIDER_ID = Key.create(GENERATE_SYNTHETIC_PROVIDER_NAME);

    static final Key<SyntheticRunTargetSession> SYNTHETIC_BUILD_SESSION = Key.create("bazel.run.synthetic.build_file.vfile");

    public GenerateSyntheticTargetRunTaskProvider() {
    }

    @Override
    public String getId() {
        return GENERATE_SYNTHETIC_PROVIDER_NAME;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(GENERATE_SYNTHETIC_PROVIDER_NAME);
    }

    // consulo: BeforeRunFragment requires an icon, upstream BeforeRunTaskProvider has none by default
    @Override
    public @Nullable Image getIcon(RunConfiguration configuration) {
        return null;
    }

    @Override
    public Class<Task> getStateClass() {
        return Task.class;
    }

    @Override
    public Task createTask(RunConfiguration runConfiguration) {
        return new Task();
    }

    // consulo: BeforeRunFragment.executeAsync() replaces executeTask(); a task returning false cancels the run like upstream
    @Override
    public CompletableFuture<?> executeAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        try {
            if (executeTask(context, configuration, environment, task)) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new CancellationException());
        }
        catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) throws IOException {
        Project project = environment.getProject();
        TaskState taskState = task.getTaskState();
        Label targetLabel = Label.parse(taskState.target);
        BuildTargetInfo target = TargetStorage.getTargetInfo(TargetStorage.getTargetStorage(project), targetLabel);
        if (target == null) {
            return false;
        }
        Language language = Language.findLanguageByID(taskState.language);
        if (language == null) {
            return false;
        }
        SyntheticRunTargetTemplateGenerator generator = null;
        for (SyntheticRunTargetTemplateGenerator it : SyntheticRunTargetTemplateGenerator.allForLanguage(language)) {
            if (it.isSupported(target)) {
                generator = it;
                break;
            }
        }
        if (generator == null) {
            return false;
        }
        SyntheticRunTargetTemplate template =
            generator.createSyntheticTemplate(target, new SyntheticRunTargetTemplateGenerator.Params(taskState.params));
        Path buildFilePath = getRootDir(project).toNioPath()
            .resolve(Constants.DOT_BAZELBSP_DIR_NAME)
            .resolve("synthetic_targets")
            .resolve(template.buildFilePath())
            .resolve("BUILD");

        BazelRunConfiguration bazelConfiguration = (BazelRunConfiguration) configuration;
        Files.createDirectories(Objects.requireNonNull(buildFilePath.getParent()));
        Files.writeString(buildFilePath, template.buildFileContent());

        // See SyntheticRunExecutionListener
        SyntheticRunTargetSession session = new SyntheticRunTargetSession(/* buildFilePath = */ buildFilePath);
        bazelConfiguration.putUserData(SYNTHETIC_BUILD_SESSION, session);

        if (BazelFeatureFlags.getSyntheticRunDisableVisibilityCheck()) {
            bazelConfiguration.disableVisibilityCheck();
        }

        return true;
    }

    // consulo: upstream BeforeRunTask<Task>(GENERATE_SYNTHETIC_PROVIDER_ID) + PersistentStateComponent<TaskState>; Consulo before
    // run tasks are beans serialized by XmlSerializer, the state is a bean property
    public static class Task extends BeforeRunTask implements SyntheticRunTaskMarker {
        private TaskState myTaskState = new TaskState();

        @Property(surroundWithTag = false)
        public TaskState getTaskState() {
            return myTaskState;
        }

        public void setTaskState(TaskState taskState) {
            myTaskState = taskState;
        }

        @Transient
        public TaskState getState() {
            return myTaskState;
        }

        public void loadState(TaskState state) {
            myTaskState = state;
        }
    }

    public static class TaskState {
        @Attribute("target")
        public String target = "";

        @Attribute("language")
        public String language = Language.ANY.getID();

        @Attribute("params")
        public @Nullable String params = "";
    }

    public interface SyntheticRunTaskMarker {
    }
}

record SyntheticRunTargetSession(
    Path buildFilePath
) {
}
