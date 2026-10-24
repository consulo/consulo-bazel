package org.jetbrains.bazel.action;

import consulo.application.Application;
import consulo.document.FileDocumentManager;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;

import java.util.concurrent.CompletableFuture;

// consulo: upstream Kotlin top-level function; the class keeps the upstream file name
public final class SaveAllFiles {
    private SaveAllFiles() {
    }

    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public static CompletableFuture<?> saveAllFiles() {
        Application application = Application.get();
        CoroutineScope scope = CoroutineScope.of(application.coroutineContext());
        // consulo: UIAction (upstream `withContext(Dispatchers.EDT)`) needs the UIAccess in the scope
        scope.putCopyableUserData(UIAccess.KEY, application.getLastUIAccess());
        CompletableFuture<?> saved = Coroutine
            .first(UIAction.<Object, Object>apply(input -> {
                FileDocumentManager.getInstance().saveAllDocuments();
                return input;
            }))
            .runAsync(scope, null)
            .toFuture();
        // Flush VFS before calling Bazel: https://blog.jetbrains.com/platform/2026/06/async-vfs-content-writes-what-plugin-authors-need-to-know/
        // TODO consulo: ManagingFS.flushPendingUpdatesOrNotify() does not exist in Consulo (it would run after `saved` completes)
        // ManagingFS.getInstance().flushPendingUpdatesOrNotify();
        return saved;
    }
}
