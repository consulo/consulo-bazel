package org.jetbrains.bazel.ui.widgets;

import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.codeEditor.Editor;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// consulo: upstream Kotlin top-level function; the class keeps the upstream file name
public final class ChooseTarget {
    private ChooseTarget() {
    }

    public static CompletableFuture<@Nullable Label> chooseTarget(List<Label> receiver, @Nullable Editor editor) {
        if (editor == null) {
            return CompletableFuture.completedFuture(receiver.isEmpty() ? null : receiver.get(0)); // Can't show the popup without a parent component
        }
        if (receiver.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (receiver.size() == 1) {
            return CompletableFuture.completedFuture(receiver.get(0));
        }

        // consulo: withContext(Dispatchers.EDT) { suspendCoroutine { } } -> a future completed by the popup callbacks
        CompletableFuture<@Nullable Label> continuation = new CompletableFuture<>();
        Application.get().getLastUIAccess().execute(() -> {
            SimpleReference<JBPopup> popup = new SimpleReference<>();
            boolean[] continuationResumed = {false};

            // consulo: com.intellij.ui.list.buildTargetPopup does not exist; the same popup chooser with a
            // TargetPresentation-like renderer (text + icon) and speed search by the presentable text
            JBPopup created = JBPopupFactory.getInstance()
                .<Label>createPopupChooserBuilder(receiver)
                .setRenderer(SimpleListCellRenderer.<Label>create((label, target, index) -> {
                    label.setText(target.toString());
                    label.setIcon(BazelPluginIcons.bazel());
                }))
                .setNamerForFiltering(Label::toString)
                .setItemChosenCallback(targetId -> {
                    if (!continuationResumed[0]) {
                        continuationResumed[0] = true;
                        continuation.complete(targetId);
                    }
                    JBPopup shownPopup = popup.get();
                    if (shownPopup != null) {
                        shownPopup.closeOk(null);
                    }
                })
                .setTitle(BazelPluginLocalize.widgetTargetChoose().get())
                .setCancelCallback(() -> {
                    if (!continuationResumed[0]) {
                        continuationResumed[0] = true;
                        continuation.complete(null);
                    }
                    return true;
                })
                // Don't close upon selection, because otherwise the cancel callback is called before the item selection callback
                .setCloseOnEnter(false)
                .setCancelOnClickOutside(true)
                .createPopup();
            popup.set(created);
            editor.showPopupInBestPositionFor(created);
        });
        return continuation;
    }
}
