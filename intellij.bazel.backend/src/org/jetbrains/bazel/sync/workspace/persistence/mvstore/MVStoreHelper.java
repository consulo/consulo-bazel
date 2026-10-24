package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import consulo.logging.Logger;
import org.h2.mvstore.MVStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

// consulo: upstream Kotlin top-level functions
final class MVStoreHelper {
    private MVStoreHelper() {
    }

    static MVStore createOrResetMvStore(Logger log, Path file) {
        try {
            // consulo: Kotlin Path.createParentDirectories()
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            return tryOpenMvStore(log, file);
        }
        catch (Throwable e) {
            log.warn("Error loading snapshot storage", e);
        }
        try {
            Files.deleteIfExists(file);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return tryOpenMvStore(log, file);
    }

    private static MVStore tryOpenMvStore(Logger log, Path file) {
        MVStore store = new MVStore.Builder()
            .fileName(file.toAbsolutePath().toString())
            .backgroundExceptionHandler((thread, e) -> log.warn("Background error in snapshot storage", e))
            .autoCommitDisabled()
            .cacheSize(16)
            .open();
        store.setVersionsToKeep(0);
        return store;
    }
}
