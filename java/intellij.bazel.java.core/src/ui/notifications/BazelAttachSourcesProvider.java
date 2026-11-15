// consulo: package org.jetbrains.bazel.ui.notifications is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.ui.notifications.java;

import com.intellij.java.impl.codeInsight.AttachSourcesProvider;
import com.intellij.java.impl.openapi.roots.ui.configuration.PathUIUtils;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.event.ComponentEvent;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * See https://github.com/bazelbuild/bazel/issues/10692
 */
@ExtensionImpl
public class BazelAttachSourcesProvider implements AttachSourcesProvider {
    // consulo: ActionCallback -> CompletableFuture (Consulo AttachSourcesAction API)
    private static class BazelAttachSourcesAction implements AttachSourcesProvider.AttachSourcesAction {
        BazelAttachSourcesAction() {
        }

        @Override
        public LocalizeValue getName() {
            return BazelPluginLocalize.sourcesAttachActionText();
        }

        @Override
        public LocalizeValue getBusyText() {
            return BazelPluginLocalize.sourcesPendingText();
        }

        @Override
        public CompletableFuture<?> perform(List<LibraryOrderEntry> orderEntries, ComponentEvent<Component> e) {
            List<Library> libraries = new ArrayList<>(new LinkedHashSet<>(
                orderEntries.stream().map(LibraryOrderEntry::getLibrary).filter(Objects::nonNull).toList()
            ));
            return performInternal(libraries);
        }

        // consulo: upstream perform(libraryEntities: Collection<LibraryEntity>, project) is the workspace model variant,
        //   not used by Consulo

        private CompletableFuture<?> performInternal(List<Library> libraries) {
            CompletableFuture<@Nullable Void> result = new CompletableFuture<>();
            Application.get().executeOnPooledThread(() -> {
                try {
                    List<Library> libsWithEmptySources = new ArrayList<>();
                    List<Library> libsWithSources = new ArrayList<>();
                    for (Library library : libraries) {
                        if (library.getFiles(SourcesOrderRootType.ID).length == 0) {
                            libsWithEmptySources.add(library);
                        }
                        else {
                            libsWithSources.add(library);
                        }
                    }
                    if (!libsWithEmptySources.isEmpty()) {
                        String missingSources = libsWithEmptySources.stream()
                            .map(it -> Objects.requireNonNullElse(it.getName(), ""))
                            .collect(Collectors.joining(",\n"));
                        showError(missingSources);
                    }
                    List<Pair<Library.ModifiableModel, VirtualFile[]>> modelsToCommit = new ArrayList<>();
                    for (Library library : libsWithSources) {
                        VirtualFile[] availableSources = library.getFiles(SourcesOrderRootType.ID);
                        Pair<Library.ModifiableModel, VirtualFile[]> it = obtainModelWithAddedSources(library, availableSources);
                        if (it != null) {
                            modelsToCommit.add(it);
                        }
                    }
                    if (!modelsToCommit.isEmpty()) {
                        // consulo: WriteAction.run -> WriteAction.runAndWait (we are on a pooled thread)
                        WriteAction.runAndWait(() -> {
                            for (Pair<Library.ModifiableModel, VirtualFile[]> pair : modelsToCommit) {
                                Library.ModifiableModel model = pair.getFirst();
                                for (VirtualFile it : pair.getSecond()) {
                                    model.addRoot(it.getUrl(), SourcesOrderRootType.ID);
                                }
                                model.commit();
                            }
                            result.complete(null);
                        });
                    }
                    else {
                        result.complete(null);
                    }
                }
                catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
            return result;
        }

        private void showError(String target) {
            BazelBalloonNotifier.error(
                BazelPluginLocalize.sourcesFilesNotResolved(),
                BazelPluginLocalize.errorMessageFailedToResolveSources0(target)
            );
        }

        private static @Nullable Pair<Library.ModifiableModel, VirtualFile[]> obtainModelWithAddedSources(
            Library receiver,
            VirtualFile[] availableSources
        ) {
            // consulo: invokeAndWaitIfNeeded { LibrarySourceRootDetectorUtil.scanAndSelectDetectedJavaSourceRoots(null, sources) } ->
            //   PathUIUtils.scanAndSelectDetectedJavaSourceRoots (asynchronous in Consulo), started on the UI thread and awaited
            VirtualFile[] sourceRoots =
                Application.get().getLastUIAccess()
                    .giveAsync(() -> PathUIUtils.scanAndSelectDetectedJavaSourceRoots(null, null, availableSources))
                    .thenCompose(Function.identity())
                    .join();
            if (sourceRoots.length == 0) {
                return null;
            }
            return Pair.create(receiver.getModifiableModel(), sourceRoots);
        }
    }

    // consulo: upstream getLibrariesActions(libraryEntities: Collection<LibraryEntity>, psiFile) is the workspace model
    //   variant, not used by Consulo; containBazelSourcesForLibraries(libraries: Collection<LibraryEntity>) is dropped with it

    @Override
    public Collection<AttachSourcesProvider.AttachSourcesAction> getActions(
        List<LibraryOrderEntry> orderEntries,
        PsiFile psiFile
    ) {
        Project project = orderEntries.stream()
            .map(it -> it.getOwnerModule().getProject())
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
        if (org.jetbrains.bazel.config.project.isBazelProject(project) && containsBazelSourcesForEntries(orderEntries)) {
            return List.of(new BazelAttachSourcesAction());
        }
        else {
            return List.of();
        }
    }

    private boolean containsBazelSourcesForEntries(List<LibraryOrderEntry> orderEntries) {
        return orderEntries.stream().anyMatch(it -> {
            Library library = it.getLibrary();
            return library != null && library.getFiles(SourcesOrderRootType.ID).length != 0;
        });
    }
}
