package org.jetbrains.bazel.clion.debug;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consulo: upstream Kotlin top-level function; the class keeps the upstream file name
final class chooseInPopup {
    private chooseInPopup() {
    }

    /**
     * Shows a list popup with speed search and waits for the choice.
     *
     * <p>The caller is canceled when the user closes the popup without a choice.
     */
    static <T> CompletableFuture<T> chooseInPopup(
        Project project,
        LocalizeValue title,
        List<T> candidates,
        Function<T, String> textFor
    ) {
        // consulo: withContext(Dispatchers.EDT) { suspendCancellableCoroutine { } } -> a future completed by the popup step
        CompletableFuture<T> continuation = new CompletableFuture<>();
        project.getUIAccess().give(() -> {
            JBPopupFactory.getInstance()
                .createListPopup(new ChoicePopupStep<>(continuation, title, candidates, textFor))
                .showCenteredInCurrentWindow(project);
        });
        // consulo: withContext(Dispatchers.EDT) returns to the dispatcher of the caller, the choice is handed back on a pooled thread
        return continuation.thenApplyAsync(it -> it, Application.get().getInstance(ApplicationConcurrency.class).getExecutorService());
    }
}

final class ChoicePopupStep<T> extends BaseListPopupStep<T> {
    private final CompletableFuture<T> myContinuation;

    private final Function<T, String> myTextFor;

    ChoicePopupStep(
        CompletableFuture<T> continuation,
        LocalizeValue title,
        List<T> candidates,
        Function<T, String> textFor
    ) {
        super(title.get(), candidates);
        myContinuation = continuation;
        myTextFor = textFor;
    }

    @Override
    public String getTextFor(T value) {
        return myTextFor.apply(value);
    }

    @Override
    public @Nullable PopupStep onChosen(T selectedValue, boolean finalChoice) {
        myContinuation.complete(selectedValue);
        return super.onChosen(selectedValue, true);
    }

    @Override
    public boolean isSpeedSearchEnabled() {
        return true;
    }

    @Override
    public void canceled() {
        myContinuation.completeExceptionally(new CancellationException());
        super.canceled();
    }
}
