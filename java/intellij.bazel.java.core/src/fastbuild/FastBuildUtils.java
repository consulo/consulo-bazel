package org.jetbrains.bazel.fastbuild;

import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.impl.DebuggerSession;
import com.intellij.java.debugger.impl.HotSwapFile;
import com.intellij.java.debugger.impl.HotSwapManager;
import com.intellij.java.debugger.impl.settings.DebuggerSettings;
import com.intellij.java.debugger.impl.ui.HotSwapProgressImpl;
import com.intellij.java.debugger.impl.ui.RunHotswapDialog;
import com.intellij.java.debugger.localize.JavaDebuggerLocalize;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.scope.CompileScope;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.ProcessOutputType;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.ExecUtils;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.tasks.BuildTargetTask;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelpers;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.JvmToolchainInfo;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FastBuildUtils {
    private FastBuildUtils() {
    }

    private static final Logger logger = Logger.getInstance(FastBuildUtils.class);

    // consulo: upstream returns Promise<ProjectTaskRunner.Result>; Consulo has no ProjectTaskRunner, the future completes with true
    // for TaskRunnerResults.SUCCESS and false for TaskRunnerResults.FAILURE
    public static CompletableFuture<Boolean> fastBuildFilesPromise(Project project, List<VirtualFile> files) {
        CompletableFuture<Boolean> promise = new CompletableFuture<>();
        FastBuildStatusService fastBuildService = FastBuildStatusService.getInstance(project);
        BazelCoroutineService.getInstance(project).start(() -> {
            CompletableFuture<?> build;
            try {
                fastBuildService.startFastBuild();
                build = fastBuildFiles(project, files);
            }
            catch (Exception e) {
                build = CompletableFuture.failedFuture(e);
            }
            return build.<@Nullable Object>handle((result, error) -> {
                if (error == null) {
                    promise.complete(true);
                }
                else {
                    Throwable e = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    promise.complete(false);
                    // consulo: the "HotSwap Messages" group of consulo-java (HotSwapProgressImpl.NOTIFICATION_GROUP) is not public
                    NotificationService.getInstance()
                        .newError(BazelNotificationGroupContributor.BAZEL)
                        .title(BazelPluginLocalize.widgetFastbuildErrorTitle())
                        .content(LocalizeValue.ofNullable(e.getLocalizedMessage()))
                        .important(true)
                        .notify(project);
                }
                return null;
            });
        });
        promise
            .whenComplete((result, error) -> {
                if (error == null) {
                    fastBuildService.finishFastBuild(
                        new FastBuildStatus(
                            /* workspaceRoot = */ org.jetbrains.bazel.config.project.getRootDir(project).toNioPath(),
                            /* status = */ FastBuildActionStatus.SUCCESS
                        )
                    );
                }
                else {
                    fastBuildService.finishFastBuild(
                        new FastBuildStatus(
                            /* workspaceRoot = */ org.jetbrains.bazel.config.project.getRootDir(project).toNioPath(),
                            /* status = */ FastBuildActionStatus.ERROR
                        )
                    );
                }
            });
        return promise;
    }

    public static CompletableFuture<?> fastBuildFiles(Project project, List<VirtualFile> files) {
        Path workspaceRoot = org.jetbrains.bazel.config.project.getRootDir(project).toNioPath();
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        FastBuildStatusService fastBuildService = FastBuildStatusService.getInstance(project);
        Map<VirtualFile, BuildTarget> buildInfos = new LinkedHashMap<>();
        for (VirtualFile file : files) {
            List<Label> targetsForFile = targetUtils.getTargetsForFile(file);
            if (targetsForFile.isEmpty()) {
                continue;
            }
            BuildTarget targetSummary = targetUtils.getTargetSummary(targetsForFile.get(0));
            if (targetSummary != null) {
                buildInfos.put(file, targetSummary);
            }
        }

        if (buildInfos.isEmpty()) {
            return CompletableFuture.failedFuture(new ExecutionException(BazelPluginLocalize.widgetFastbuildErrorNoTargets()));
        }

        // consulo: the suspend loop is a chain of futures, one per entry; a thrown ExecutionException fails the chain (wrapped in a
        // CompletionException)
        CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
        for (Map.Entry<VirtualFile, BuildTarget> entry : buildInfos.entrySet()) {
            chain = chain.thenCompose(ignored -> {
                VirtualFile inputFile = entry.getKey();
                Label id = BuildTarget.getId(entry.getValue());
                String canonicalFilePath = inputFile.getCanonicalPath();
                String canonicalWorkspaceRootPath = Objects.requireNonNull(FileUtil.toCanonicalPath(workspaceRoot.toString()));
                if (!(canonicalFilePath != null && canonicalFilePath.startsWith(canonicalWorkspaceRootPath))) {
                    throw new CompletionException(
                        new ExecutionException(
                            BazelPluginLocalize.widgetFastbuildErrorWrongCanonicalPath(
                                canonicalFilePath == null ? "" : canonicalFilePath,
                                canonicalWorkspaceRootPath
                            )
                        )
                    );
                }
                // bazel-bin mirrors the package path of a main workspace target
                if (!id.isMainWorkspace()) {
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                }
                String relativePath = id.packagePath().toString();
                boolean isLib = targetUtils.isLibrary(id);
                WorkspaceSnapshot snapshot = WorkspaceHelpers.getWorkspaceSnapshotOrNull(project);
                Path bazelBin = snapshot == null ? null : snapshot.bazelInfo().bazelBin();
                if (bazelBin == null) {
                    throw new CompletionException(new ExecutionException(BazelPluginLocalize.widgetFastbuildErrorMissingPath()));
                }
                Path targetJar = Path.of(bazelBin + "/" + relativePath + "/" + (isLib ? "lib" : "") + id.getTargetName() + ".jar");

                fastBuildService.startFastBuildTarget();

                Path targetJarParent = Objects.requireNonNull(targetJar.getParent());
                Path originalParamsFile = targetJarParent.resolve(targetJar.getFileName().toString() + "-0.params");
                Path originalParamsFile1 = targetJarParent.resolve(targetJar.getFileName().toString() + "-1.params");

                if (Files.notExists(originalParamsFile)) {
                    List<Label> ids = new ArrayList<>();
                    for (BuildTarget it : buildInfos.values()) {
                        ids.add(BuildTarget.getId(it));
                    }
                    BazelCoroutineService.getInstance(project).start(() -> BuildTargetTask.runBuildTargetTask(ids, project));
                    throw new CompletionException(new ExecutionException(BazelPluginLocalize.widgetFastbuildErrorParamsNotFound()));
                }

                // consulo: upstream withContext(Dispatchers.EDT) { writeIntentReadAction { saveAllDocuments() } }; the compile task is
                // started on the UI thread too, because CompilerManager.executeTask() saves the documents itself
                return project.getUIAccess().<Boolean>giveAsync(() -> {
                    FileDocumentManager.getInstance().saveAllDocuments();

                    // consulo: upstream CompilerTask/CompileContextImpl/OneProjectItemCompileScope are internal in Consulo, the public
                    // CompilerManager.executeTask() creates them for a CompileTask
                    CompileScope compileScope =
                        CompilerManager.getInstance(project).createFilesCompileScope(List.of(inputFile.toNioPath()));
                    CompilerManager.getInstance(project).executeTask(
                        compileContext -> {
                            // consulo: upstream `var fastBuildTargetStatus: FastBuildTargetStatus? = null` captured by the task
                            FastBuildStatus.@Nullable FastBuildTargetStatus[] fastBuildTargetStatus =
                                new FastBuildStatus.FastBuildTargetStatus[1];
                            try {
                                // consulo: upstream ProgressManager.getInstance().progressIndicator
                                ProgressIndicator indicator = compileContext.getProgressIndicator();

                                indicator.checkCanceled();

                                Path tempDir = Files.createTempDirectory("cdb-fast-compile");
                                logger.debug("Using '" + tempDir + "' for fast compile");
                                Path outputJar = tempDir.resolve("output.jar");

                                Path paramsFile0 =
                                    updateAndWriteCompile0Params(
                                        originalParamsFile,
                                        tempDir,
                                        outputJar,
                                        Path.of(Objects.requireNonNull(inputFile.getCanonicalPath())),
                                        Path.of(canonicalWorkspaceRootPath),
                                        targetJar
                                    );
                                Path paramsFile1 =
                                    updateAndWriteCompile1Params(
                                        originalParamsFile1,
                                        tempDir
                                    );

                                indicator.checkCanceled();

                                JvmToolchainInfo toolchainInfo;
                                try {
                                    toolchainInfo =
                                        JvmToolchainInfoService
                                            .getInstance(project)
                                            .getOrQueryJvmToolchainInfo(project, id);
                                }
                                catch (Exception e) {
                                    throw new ExecutionException(BazelPluginLocalize.widgetFastbuildErrorNullJvmToolchain());
                                }

                                List<String> arguments = new ArrayList<>();
                                arguments.add(
                                    toolchainInfo.java_home() + File.separator + "bin" + File.separator +
                                        ExecUtils.calculateExecutableName("java")
                                );
                                arguments.addAll(toolchainInfo.jvm_opts());
                                arguments.add("-jar");
                                arguments.add(toolchainInfo.toolchain_path());
                                arguments.add("@" + paramsFile0);
                                arguments.add("@" + paramsFile1);

                                WorkspaceSnapshot workspaceSnapshot = WorkspaceHelpers.getWorkspaceSnapshotOrNull(project);
                                BazelInfo bazelInfo = workspaceSnapshot == null ? null : workspaceSnapshot.bazelInfo();
                                if (bazelInfo == null) {
                                    throw new ExecutionException(BazelPluginLocalize.widgetFastbuildErrorNullBazelExecPath());
                                }
                                GeneralCommandLine command =
                                    new GeneralCommandLine(arguments).withWorkingDirectory(bazelInfo.execRoot());
                                // consulo: upstream OSProcessHandler is internal in Consulo
                                ProcessHandler handler =
                                    Application.get().getInstance(ProcessHandlerBuilderFactory.class).newBuilder(command).build();
                                handler.addProcessListener(
                                    new ProcessListener() {
                                        // TODO: adapt EclipseOutputParser and JavacOutputParser and group parse errors properly
                                        final String JAVAC_WARNING = "warning:";
                                        final String JAVAC_ERROR = "error:";
                                        final String ECLIPSE_PROBLEM_SEPARATOR = "----------";
                                        final String ECLIPSE_WARNING = ". WARNING in ";
                                        final String ECLIPSE_ERROR = ". ERROR in ";

                                        boolean isWarning = false;

                                        @Override
                                        public void onTextAvailable(ProcessEvent event, Key outputType) {
                                            if (outputType == ProcessOutputType.STDERR) {
                                                if (event.getText().contains(ECLIPSE_PROBLEM_SEPARATOR)) {
                                                    return;
                                                }
                                                if (event.getText().contains(JAVAC_WARNING) || event.getText().contains(ECLIPSE_WARNING)) {
                                                    isWarning = true;
                                                }
                                                else if (event.getText().contains(JAVAC_ERROR) || event.getText().contains(ECLIPSE_ERROR)) {
                                                    isWarning = false;
                                                }
                                                CompilerMessageCategory compilerCategory =
                                                    isWarning ? CompilerMessageCategory.WARNING : CompilerMessageCategory.ERROR;

                                                String[] parts = event.getText().split(":", -1);
                                                Integer line = null;
                                                try {
                                                    line = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : null;
                                                }
                                                catch (NumberFormatException ignored) {
                                                    // consulo: upstream toIntOrNull()
                                                }
                                                if (parts.length >= 4 && line != null) {
                                                    String error = parts[3];
                                                    // consulo: upstream CompilerMessageImpl(project, category, error, inputFile, line, 0, null)
                                                    compileContext.newMessage(compilerCategory, LocalizeValue.of(error))
                                                        .url(inputFile.getUrl())
                                                        .position(line, 0)
                                                        .add();
                                                }
                                                else {
                                                    // consulo: upstream CompilerMessageImpl(project, category, event.text)
                                                    compileContext.newMessage(compilerCategory, LocalizeValue.of(event.getText())).add();
                                                }
                                            }
                                            else if (outputType == ProcessOutputType.STDOUT) {
                                                compileContext.newMessage(
                                                    CompilerMessageCategory.INFORMATION,
                                                    LocalizeValue.of(event.getText())
                                                ).add();
                                            }
                                        }
                                    }
                                );
                                handler.startNotify();
                                Integer exitCode = null;
                                if (handler.waitFor()) {
                                    exitCode = handler.getExitCode();
                                }
                                if (exitCode == null || exitCode != 0) {
                                    // consulo: CompilerManager.executeTask() sets the end compilation stamp from the error count
                                    // compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
                                    // tempDir.delete()
                                    fastBuildTargetStatus[0] =
                                        new FastBuildStatus.FastBuildTargetStatus(
                                            /* inputFile = */ inputFile,
                                            /* targetJar = */ targetJar,
                                            /* status = */ FastBuildActionStatus.ERROR
                                        );
                                    return true;
                                }
                                // compileTask.setEndCompilationStamp(ExitStatus.SUCCESS, System.currentTimeMillis())
                                fastBuildTargetStatus[0] =
                                    new FastBuildStatus.FastBuildTargetStatus(
                                        /* inputFile = */ inputFile,
                                        /* targetJar = */ targetJar,
                                        /* status = */ FastBuildActionStatus.SUCCESS
                                    );
                                processAndHotswapOutput(tempDir, outputJar, project);
                            }
                            catch (ExecutionException e) {
                                // consulo: upstream CompilerMessageImpl(project, CompilerMessageCategory.ERROR, e.message)
                                compileContext.newMessage(
                                    CompilerMessageCategory.ERROR,
                                    LocalizeValue.ofNullable(e.getMessage())
                                ).add();
                                fastBuildTargetStatus[0] =
                                    new FastBuildStatus.FastBuildTargetStatus(
                                        /* inputFile = */ inputFile,
                                        /* targetJar = */ targetJar,
                                        /* status = */ FastBuildActionStatus.ERROR
                                    );
                                // compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
                                return true;
                            }
                            catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            finally {
                                if (fastBuildTargetStatus[0] == null) {
                                    fastBuildTargetStatus[0] =
                                        new FastBuildStatus.FastBuildTargetStatus(
                                            /* inputFile = */ inputFile,
                                            /* targetJar = */ targetJar,
                                            /* status = */ FastBuildActionStatus.UNKNOWN
                                        );
                                }
                                fastBuildService.finishFastBuildTarget(Objects.requireNonNull(fastBuildTargetStatus[0]));
                            }
                            return true;
                        },
                        compileScope,
                        BazelPluginLocalize.widgetFastbuildRecompile(),
                        null
                    );
                    return Boolean.TRUE;
                });
            });
        }
        return chain;
    }

    // consulo: Kotlin function type (ips: BufferedReader, os: OutputStreamWriter) -> Unit that may throw IOException
    @FunctionalInterface
    public interface ParamsFileHandler {
        void handle(BufferedReader ips, OutputStreamWriter os) throws IOException;
    }

    // consulo: upstream local extension fun OutputStreamWriter.writeLn(line: String) of both params writers
    private static void writeLn(OutputStreamWriter receiver, String line) throws IOException {
        receiver.write(line + "\n");
    }

    /**
     * Copies the original params file updating certain fields to reduce compile time
     * and point to a different output
     */
    public static Path updateAndWriteCompile0Params(
        Path originalParams,
        Path tempDir,
        Path outputJar,
        Path inputFile,
        Path workspaceRoot,
        Path targetJar
    ) throws IOException {
        return updateAndWriteCompileParams("compile.jar-0.params", originalParams, tempDir, (BufferedReader ips, OutputStreamWriter os) -> {
            String line = ips.readLine();
            Path sourceFile = workspaceRoot.relativize(inputFile);

            while (line != null) {
                switch (line) {
                    case "--output" -> {
                        writeLn(os, line);
                        writeLn(os, outputJar.toString());
                        ips.readLine();
                    }

                    case "--native_header_output" -> {
                        writeLn(os, line);
                        writeLn(os, tempDir.resolve("output-native-header.jar").toString());
                        ips.readLine();
                    }

                    case "--generated_sources_output" -> {
                        writeLn(os, line);
                        writeLn(os, tempDir.resolve("output-generated-sources.jar").toString());
                        ips.readLine();
                    }

                    case "--output_manifest_proto" -> {
                        writeLn(os, line);
                        writeLn(os, tempDir.resolve("output.jar_manifest_proto").toString());
                        ips.readLine();
                    }

                    case "--output_deps_proto" -> {
                        writeLn(os, line);
                        writeLn(os, tempDir.resolve("output.jdeps").toString());
                        ips.readLine();
                    }

                    case "--sources" -> {
                        writeLn(os, line);
                        line = ips.readLine();
                        writeLn(os, sourceFile.toString());
                        while (line != null && !line.startsWith("--")) {
                            line = ips.readLine();
                        }
                        continue;
                    }

                    case "--source_jars" -> {
                        line = ips.readLine();
                        while (line != null && !line.startsWith("--")) {
                            line = ips.readLine();
                        }
                        continue;
                    }

                    case "--strict_java_deps" -> {
                        line = ips.readLine();
                        while (line != null && !line.startsWith("--")) {
                            line = ips.readLine();
                        }
                        continue;
                    }

                    default -> {
                        line = processLine(line);
                        writeLn(os, line);
                    }
                }
                line = ips.readLine();
            }

            writeLn(os, "--classpath");
            // Add the modules jar to the dependencies
            if (Files.notExists(targetJar)) {
                logger.error("Bazel module jar not found: " + targetJar);
            }
            writeLn(os, targetJar.toString());
        });
    }

    public static Path updateAndWriteCompile1Params(Path originalParams, Path tempDir) throws IOException {
        return updateAndWriteCompileParams("compile.jar-1.params", originalParams, tempDir, (BufferedReader ips, OutputStreamWriter os) -> {
            String line = ips.readLine();

            while (line != null) {
                line = processLine(line);
                writeLn(os, line);
                line = ips.readLine();
            }
        });
    }

    public static Path updateAndWriteCompileParams(
        String paramFileName,
        Path originalParams,
        Path tempDir,
        ParamsFileHandler handleParamsFile
    ) throws IOException {
        Path params = tempDir.resolve(paramFileName);
        try (BufferedReader ips = Files.newBufferedReader(originalParams, StandardCharsets.UTF_8)) {
            try (OutputStreamWriter os = new OutputStreamWriter(Files.newOutputStream(params), StandardCharsets.UTF_8)) {
                handleParamsFile.handle(ips, os);
            }
        }
        return params;
    }

    private static String processLine(String line) {
        String resultLine = line;
        String jarSuffix = ".jar";
        String hjarSuffix = "-hjar" + jarSuffix;
        int expectedIndex = resultLine.length() - hjarSuffix.length();
        int actualIndex = resultLine.indexOf(hjarSuffix, expectedIndex);
        if (actualIndex >= 0 && actualIndex == expectedIndex) {
            resultLine = resultLine.substring(0, actualIndex) + jarSuffix;
        }
        return resultLine;
    }

    /**
     * Unzips the jar and uses the files within to hotswap
     */
    public static void processAndHotswapOutput(
        Path tempDir,
        Path outputJar,
        Project project
    ) {
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, BazelPluginLocalize.widgetFastbuildHotswaping(), true) {
                @Override
                public void run(ProgressIndicator indicator) {
                    Path unzip = tempDir.resolve("unzip");
                    unzip.toFile().mkdir();
                    List<String> names = new ArrayList<>();
                    try (ZipFile zipFile = new ZipFile(outputJar.toFile())) {
                        for (ZipEntry it : Collections.list(zipFile.entries())) {
                            if (it.isDirectory() || !it.getName().endsWith(".class")) {
                                continue;
                            }
                            Path output = unzip.resolve(it.getName());
                            Objects.requireNonNull(output.getParent()).toFile().mkdirs();
                            zipFile.getInputStream(it).transferTo(Files.newOutputStream(output));
                            names.add(it.getName());
                        }
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    Map<String, HotSwapFile> files = new LinkedHashMap<>();
                    for (String it : names) {
                        String key = it.replace(File.separatorChar, '.');
                        if (key.endsWith(".class")) {
                            key = StringUtil.substringBeforeLast(key, ".");
                        }
                        // consulo: HotSwapFile takes a Path
                        files.put(key, new HotSwapFile(unzip.resolve(it)));
                    }

                    indicator.checkCanceled();

                    hotswapFile(project, files, tempDir.toFile());
                }
            }
        );
    }

    public static void hotswapFile(
        Project project,
        Map<String, HotSwapFile> hotswapMap,
        File tempDir
    ) {
        Application.get().invokeLater(() -> {
            Collection<DebuggerSession> sessionsToHotswap = getSessionsToHotswap(project);
            // consulo: HotSwapProgressImpl needs the ProgressIndicator of a running task (upstream creates it on the EDT and runs the
            // reload on a pooled thread), so the reload runs in a Task.Backgroundable
            Task.Backgroundable.queue(project, JavaDebuggerLocalize.progressHotSwapTitle(), progressIndicator -> {
                HotSwapProgressImpl progress = new HotSwapProgressImpl(project, progressIndicator);
                try {
                    Map<DebuggerSession, Map<String, HotSwapFile>> sessionMap = new LinkedHashMap<>();
                    for (DebuggerSession session : sessionsToHotswap) {
                        sessionMap.put(session, hotswapMap);
                    }
                    ProgressManager.getInstance().runProcess(
                        () -> {
                            HotSwapManager.reloadModifiedClasses(sessionMap, progress);
                        },
                        progress.getProgressIndicator()
                    );
                }
                finally {
                    triggerHotswapSuccessNotification(project, progress);
                    progress.finished();
                    tempDir.delete();
                }
            });
        });
    }

    private static void triggerHotswapSuccessNotification(Project project, HotSwapProgressImpl progress) {
        try {
            // TODO consulo: Consulo's HotSwapProgressImpl has no hasErrors(), the reflective call fails and only logs a warning
            Method hasErrorsMethod = HotSwapProgressImpl.class.getDeclaredMethod("hasErrors");
            hasErrorsMethod.setAccessible(true);
            boolean hasErrorsResult = (Boolean) hasErrorsMethod.invoke(progress);
            // Send notifications for hotswap completed without errors
            if (!hasErrorsResult) {
                // consulo: the "HotSwap Messages" group of consulo-java (HotSwapProgressImpl.NOTIFICATION_GROUP) is not public
                NotificationService.getInstance()
                    .newInfo(BazelNotificationGroupContributor.BAZEL)
                    .title(BazelPluginLocalize.widgetFastbuildCompletedTitle())
                    .content(BazelPluginLocalize.widgetFastbuildCompletedDescription())
                    .important(false)
                    .icon(PlatformIconGroup.statusSuccess())
                    .notify(project);
            }
        }
        catch (Exception e) {
            logger.warn("Failed to trigger hotswap completed notification", e);
        }
    }

    private static Collection<DebuggerSession> getSessionsToHotswap(Project project) {
        List<DebuggerSession> sessions = getDebugSessions(project);
        if (sessions.isEmpty()) {
            return sessions;
        }
        if (DebuggerSettings.RUN_HOTSWAP_ASK.equals(DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE)) {
            RunHotswapDialog runHotswapDialog = new RunHotswapDialog(project, sessions, false);
            if (!runHotswapDialog.showAndGet()) {
                return List.of();
            }

            return runHotswapDialog.getSessionsToReload();
        }
        else {
            return sessions;
        }
    }

    private static List<DebuggerSession> getDebugSessions(Project project) {
        return DebuggerManagerEx
            .getInstanceEx(project)
            .getSessions()
            .stream()
            // consulo: HotSwapUIImpl.canHotSwap(session) is missing, Consulo's HotSwapUIImplListeners checks the same
            .filter(debuggerSession -> debuggerSession.isAttached() && debuggerSession.getProcess().canRedefineClasses())
            .toList();
    }
}
