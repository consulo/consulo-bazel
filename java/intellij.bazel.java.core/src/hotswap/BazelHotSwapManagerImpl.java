/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.hotswap;

import com.intellij.java.debugger.impl.DebuggerManagerEx;
import com.intellij.java.debugger.impl.DebuggerSession;
import com.intellij.java.debugger.impl.HotSwapFile;
import com.intellij.java.debugger.impl.HotSwapManager;
import com.intellij.java.debugger.impl.ui.HotSwapProgressImpl;
import com.intellij.java.debugger.localize.JavaDebuggerLocalize;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.bazel.core.BazelNotificationGroupContributor;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@ServiceImpl
@Singleton
public class BazelHotSwapManagerImpl implements BazelHotSwapManager {
    private final Project myProject;

    @Inject
    public BazelHotSwapManagerImpl(Project project) {
        myProject = project;
    }

    @Override
    public CompletableFuture<?> hotswap(BazelHotSwapManager.HotSwapEnvironment environment) {
        List<DebuggerSession> sessions = environment.getSessions();
        if (sessions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Path tempDir;
        Map<String, HotSwapFile> hotSwapClassFiles = new LinkedHashMap<>();
        JarFileManifest.Diff diff;
        try {
            Path tempRoot = Path.of(ContainerPathManager.get().getTempPath());
            tempDir = Files.createTempDirectory(tempRoot, "class_files_");
            diff = JarFileManifest.diffJarManifests(environment.getOldManifest(), environment.getNewManifest());
            Map<String, Path> localClassFiles = copyClassFilesLocally(tempDir, diff);
            for (Map.Entry<String, Path> entry : localClassFiles.entrySet()) {
                // consulo: HotSwapFile takes a Path
                hotSwapClassFiles.put(entry.getKey(), new HotSwapFile(entry.getValue()));
            }
        }
        catch (IOException | UncheckedIOException e) {
            return CompletableFuture.failedFuture(e);
        }

        LOG.info(
            "hotswap: found " + diff.perJarModifiedFiles().size() + " jar files with changed entries after compilation\n" +
                diff.perJarModifiedFiles().entrySet().stream()
                    .map(it -> "  " + it.getKey() + ": " + it.getValue().stream().sorted().collect(Collectors.joining(" ")))
                    .sorted()
                    .collect(Collectors.joining("\n"))
        );
        if (!hotSwapClassFiles.isEmpty()) {
            // consulo: HotSwapProgressImpl needs the ProgressIndicator of a running task (upstream creates a
            // BackgroundableProcessIndicator on the EDT and runs ProgressManager.runProcess with it), so the reload runs in a
            // Task.Backgroundable that owns (and disposes) the indicator
            CompletableFuture<Void> result = new CompletableFuture<>();
            Task.Backgroundable.queue(myProject, JavaDebuggerLocalize.progressHotSwapTitle(), indicator -> {
                HotSwapProgressImpl progress = new HotSwapProgressImpl(myProject, indicator);

                try {
                    ProgressManager.getInstance().runProcess(
                        () -> {
                            LOG.info(
                                "hotswap: redefining " + hotSwapClassFiles.size() + " classes: " +
                                    String.join(" ", hotSwapClassFiles.keySet())
                            );
                            Map<DebuggerSession, Map<String, HotSwapFile>> sessionMap = new LinkedHashMap<>();
                            for (DebuggerSession session : sessions) {
                                sessionMap.put(session, hotSwapClassFiles);
                            }
                            HotSwapManager.reloadModifiedClasses(
                                sessionMap,
                                progress
                            );

                            // consulo: upstream runBlockingCancellable { }
                            for (HotSwapHook hotSwapHook : Application.get().getExtensionList(HotSwapHook.class)) {
                                hotSwapHook.onHotSwap(sessions).join();
                            }

                            // TODO consulo: HotSwapStatusListener is missing in Consulo
                            // listener?.onSuccess(sessions)
                        },
                        progress.getProgressIndicator()
                    );
                    result.complete(null);
                }
                catch (CancellationException | ProcessCanceledException e) {
                    // TODO consulo: HotSwapStatusListener is missing in Consulo
                    // listener?.onCancel(sessions)
                    result.completeExceptionally(e);
                }
                catch (Exception e) {
                    // TODO consulo: HotSwapStatusListener is missing in Consulo
                    // listener?.onFailure(sessions)
                    LOG.error("Unable to hotswap", e);
                    result.complete(null);
                }
                finally {
                    progress.finished();
                    // consulo: the indicator belongs to the Task.Backgroundable, Consulo disposes it
                    // withContext(Dispatchers.EDT) { Disposer.dispose(progress.progressIndicator as Disposable) }
                    FileUtil.delete(tempDir);
                }
            });
            return result;
        }
        else {
            if (!environment.isAutoRun()) {
                // consulo: the "HotSwap Messages" group of consulo-java (HotSwapProgressImpl.NOTIFICATION_GROUP) is not public
                // TODO consulo: HotSwapStatusNotificationManager.getInstance(project).trackNotification(notification) is missing
                NotificationService.getInstance()
                    .newInfo(BazelNotificationGroupContributor.BAZEL)
                    .content(JavaDebuggerLocalize.statusHotswapUptodate())
                    .notify(myProject);
            }
            // TODO consulo: HotSwapStatusListener is missing in Consulo
            // listener?.onNothingToReload(sessions)
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public List<DebuggerSession> getCurrentDebugSessions() {
        List<DebuggerSession> result = new ArrayList<>();
        for (DebuggerSession it : DebuggerManagerEx.getInstanceEx(myProject).getSessions()) {
            if (it.isAttached() && it.getProcess().canRedefineClasses()) {
                result.add(it);
            }
        }
        return result;
    }

    /**
     * Given a per-jar map of .class files, extracts them locally and returns a map from qualified
     * class name to File, suitable for {@link HotSwapManager}.
     */
    private Map<String, Path> copyClassFilesLocally(Path tempDir, JarFileManifest.Diff manifestDiff) throws IOException {
        Map<String, Path> map = new HashMap<>();
        MultiMap<Path, String> perJarModifiedFiles = manifestDiff.perJarModifiedFiles();
        for (Path jar : perJarModifiedFiles.keySet()) {
            List<String> classes = new ArrayList<>();
            for (String it : perJarModifiedFiles.get(jar)) {
                if (it.endsWith(".class")) {
                    classes.add(it);
                }
            }
            map.putAll(
                copyClassFilesLocally(
                    tempDir,
                    jar,
                    classes
                )
            );
        }
        return map;
    }

    private Map<String, Path> copyClassFilesLocally(
        Path destination,
        Path jar,
        Collection<String> classes
    ) throws IOException {
        Map<String, Path> map = new LinkedHashMap<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            for (String path : classes) {
                JarEntry entry = jarFile.getJarEntry(path);
                if (entry == null) {
                    throw new IllegalStateException("");
                }
                Path f = destination.resolve(path.replace('/', '-'));
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    Files.copy(inputStream, f, StandardCopyOption.REPLACE_EXISTING);
                }
                map.put(deriveQualifiedClassName(path), f);
            }
        }
        return map;
    }

    /**
     * Derive the fully qualified class name from the path inside the jar.
     */
    private String deriveQualifiedClassName(String path) {
        return StringUtil.trimEnd(path, ".class").replace('/', '.');
    }

    private static final Logger LOG = Logger.getInstance(BazelHotSwapManagerImpl.class);
}
