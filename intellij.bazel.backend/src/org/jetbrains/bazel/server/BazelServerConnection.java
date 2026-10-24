package org.jetbrains.bazel.server;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The BSP connection, implementation should keep all the information
 * needed to establish and keep the connection with the server.
 */
public interface BazelServerConnection {
    <T extends @Nullable Object> CompletableFuture<T> runWithServer(
        @Nullable TaskId taskId,
        Function<BazelServerFacade, CompletableFuture<T>> task
    );

    // consulo: upstream default value of taskId
    default <T extends @Nullable Object> CompletableFuture<T> runWithServer(Function<BazelServerFacade, CompletableFuture<T>> task) {
        return runWithServer(null, task);
    }

    // consulo: upstream Kotlin extension property Project.connection
    static BazelServerConnection getConnection(Project receiver) {
        return BazelServerService.getInstance(receiver).getConnection();
    }

    @ServiceAPI(ComponentScope.PROJECT)
    interface BazelServerService {
        BazelServerConnection getConnection();

        static BazelServerService getInstance(Project project) {
            return project.getInstance(BazelServerService.class);
        }
    }
}
