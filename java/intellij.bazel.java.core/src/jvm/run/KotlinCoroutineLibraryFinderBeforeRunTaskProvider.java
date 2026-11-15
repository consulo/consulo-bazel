package org.jetbrains.bazel.jvm.run;

import com.intellij.java.debugger.impl.engine.AsyncStacksUtils;
import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.OrderEnumerator;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.SemVer;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PathsList;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.settings.bazel.BazelJVMProjectSettings;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// consulo: IntelliJ BeforeRunTaskProvider is Consulo's BeforeRunFragment (stepsBeforeRunProvider -> project @ExtensionImpl)
@ExtensionImpl
public class KotlinCoroutineLibraryFinderBeforeRunTaskProvider
    implements BeforeRunFragment<KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task> {
    private static final String PROVIDER_NAME = "KotlinCoroutineLibraryFinderBeforeRunTaskProvider";

    // consulo: Consulo before run tasks are identified by their state class, the key is kept for upstream callers
    @SuppressWarnings("unused")
    private static final Key<Task> PROVIDER_ID = Key.create(PROVIDER_NAME);

    public KotlinCoroutineLibraryFinderBeforeRunTaskProvider() {
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(PROVIDER_NAME);
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

    // consulo: upstream BeforeRunTask<Task>(PROVIDER_ID); Consulo before run tasks are identified by their class
    public static class Task extends BeforeRunTask {
    }

    @Override
    public @Nullable Task createTask(RunConfiguration runConfiguration) {
        if (!(runConfiguration instanceof BazelRunConfiguration bazelRunConfiguration)) {
            return null;
        }
        Project project = bazelRunConfiguration.getProject();
        if (!BazelJVMProjectSettings.getBazelJVMProjectSettings(project).enableKotlinCoroutineDebug()) {
            return null;
        }
        return new Task();
    }

    // consulo: BeforeRunFragment.executeAsync() replaces executeTask()
    @Override
    public CompletableFuture<?> executeAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        return executeTask(context, configuration, environment, task);
    }

    /**
     * always return true for this task as it is just an add-on and should not block the debugging process
     */
    // consulo: upstream returns a Boolean after runBlockingMaybeCancellable { withBackgroundProgress { } }, here the future of it
    public CompletableFuture<Boolean> executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        Task task
    ) {
        BazelRunConfiguration runConfiguration = BazelRunConfiguration.get(environment);
        // skipping this task for non-debugging run config
        if (!(environment.getExecutor() instanceof DefaultDebugExecutor)) {
            return CompletableFuture.completedFuture(true);
        }
        Project project = environment.getProject();
        List<Label> targets = runConfiguration.getTargets();
        // consulo: Kotlin List.single()
        if (targets.isEmpty()) {
            throw new NoSuchElementException("List is empty.");
        }
        if (targets.size() != 1) {
            throw new IllegalArgumentException("List has more than one element.");
        }
        Label target = targets.get(0);
        BuildTarget targetInfo = TargetStorage.getTargetStorage(project).getTargetSummary(target);
        if (targetInfo == null) {
            return CompletableFuture.completedFuture(true);
        }
        if (!JavaLanguageClass.includesKotlin(targetInfo.getKind()) || !targetInfo.getKind().isExecutable()) {
            return CompletableFuture.completedFuture(true);
        }
        // consulo: withBackgroundProgress -> Task.Backgroundable.queue
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // consulo: the nested state class Task shadows consulo.application.progress.Task
        LocalizeValue title = BazelPluginLocalize.backgroundTaskDescriptionPreparingForDebuggingKotlin(target);
        consulo.application.progress.Task.Backgroundable.queue(project, title, indicator -> {
            try {
                List<VirtualFile> classPath = findClassPathByTargetLabel(project, BuildTarget.getId(targetInfo));
                VirtualFile coroutinesJarPath = findLatestCoroutinesJarRootRelativePath(classPath);
                if (coroutinesJarPath != null) {
                    calculateKotlinCoroutineParams(environment, coroutinesJarPath.getPath());
                }
                result.complete(true);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private void calculateKotlinCoroutineParams(ExecutionEnvironment environment, String coroutinesJarPath) {
        OwnJavaParameters parameters = new OwnJavaParameters();
        AsyncStacksUtils.addDebuggerAgent(parameters, environment.getProject(), false);
        parameters.getVMParametersList().add("-javaagent:" + coroutinesJarPath);
        SimpleReference<List<String>> flags = environment.getCopyableUserData(JvmRunHandler.COROUTINE_JVM_FLAGS_KEY);
        if (flags != null) {
            flags.set(parameters.getVMParametersList().getParameters());
        }
    }

    // consulo: upstream top-level functions of KotlinCoroutineLibraryFinderBeforeRunTaskProvider.kt

    public static List<String> retrieveKotlinCoroutineParams(ExecutionEnvironment environment, Project project) {
        if (!BazelJVMProjectSettings.getBazelJVMProjectSettings(project).enableKotlinCoroutineDebug()) {
            return List.of();
        }
        SimpleReference<List<String>> flags = environment.getCopyableUserData(JvmRunHandler.COROUTINE_JVM_FLAGS_KEY);
        List<String> value = flags == null ? null : flags.get();
        return value == null ? List.of() : value;
    }

    public static void attachCoroutinesDebuggerConnection(RunConfigurationBase runConfiguration) {
        for (KotlinCoroutinesHelper it : KotlinCoroutinesHelper.getEp()) {
            it.attachCoroutinesDebuggerConnection(runConfiguration.getProject(), runConfiguration);
        }
    }

    private static final SemVer MIN_COROUTINES_VERSION = Objects.requireNonNull(SemVer.parseFromText("1.3.8"));

    private static List<VirtualFile> findClassPathByTargetLabel(Project receiver, Label label) {
        List<VirtualFile> result = new ArrayList<>();
        for (Module module : receiver.getInstance(ModuleTargetService.class).findLegacyModulesByLabel(/* label = */ label)) {
            PathsList jars = new PathsList();
            // consulo: OrderEnumerator.roots(OrderRootType.CLASSES) is classes()
            OrderEnumerator
                .orderEntries(module)
                .recursively()
                .withoutSdk()
                .classes()
                .collectPaths(jars);
            result.addAll(jars.getVirtualFiles());
        }
        return result;
    }

    private static @Nullable VirtualFile findLatestCoroutinesJarRootRelativePath(List<VirtualFile> receiver) {
        Pair<VirtualFile, SemVer> found = findLatestJarByName(receiver, "kotlinx-coroutines-core-jvm");
        if (found == null) {
            found = findLatestJarByName(receiver, "kotlin-coroutines-core");
        }
        if (found == null) {
            return null;
        }
        VirtualFile path = found.getFirst();
        SemVer version = found.getSecond();
        if (version.compareTo(MIN_COROUTINES_VERSION) < 0) {
            return null;
        }
        return path;
    }

    private static @Nullable Pair<VirtualFile, SemVer> findLatestJarByName(List<VirtualFile> receiver, String name) {
        Pair<VirtualFile, SemVer> max = null;
        for (VirtualFile it : receiver) {
            Pair<VirtualFile, SemVer> pathWithVersion = extractPathWithVersionBy(it, name);
            if (pathWithVersion != null && (max == null || pathWithVersion.getSecond().compareTo(max.getSecond()) > 0)) {
                max = pathWithVersion;
            }
        }
        return max;
    }

    private static @Nullable Pair<VirtualFile, SemVer> extractPathWithVersionBy(VirtualFile receiver, String expectedName) {
        String name = receiver.getName();
        if (!name.contains(expectedName)) {
            return null;
        }
        String afterName = StringUtil.substringAfterLast(name, expectedName + "-");
        // consulo: Kotlin substringAfterLast/substringBefore return the whole string when the delimiter is missing
        String rawVersion = afterName == null ? name : afterName;
        int jarIndex = rawVersion.indexOf(".jar");
        if (jarIndex != -1) {
            rawVersion = rawVersion.substring(0, jarIndex);
        }
        SemVer version = SemVer.parseFromText(rawVersion);
        if (version == null) {
            return null;
        }
        return Pair.create(receiver, version);
    }
}
