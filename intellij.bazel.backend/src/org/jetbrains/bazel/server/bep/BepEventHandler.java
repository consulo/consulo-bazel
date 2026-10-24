package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.TaskId;

public interface BepEventHandler {
    /**
     * Allows customizing the behavior of {@link BepServer#handleEvent}.
     */
    @ExtensionAPI(ComponentScope.APPLICATION)
    interface BepEventHandlerProvider {
        // consulo: upstream EP_NAME, served by the Consulo extension point
        static ExtensionPoint<BepEventHandlerProvider> getEpName() {
            return Application.get().getExtensionPoint(BepEventHandlerProvider.class);
        }

        BepEventHandler create(BepEventHandlerContext context);
    }

    final class BepEventHandlerContext {
        private final Project myProject;
        private final TaskId myParentId;
        private final BazelTaskEventsHandler myTaskEventsHandler;
        private final DiagnosticsService myDiagnosticsService;
        private final BazelPathsResolver myBazelPathsResolver;

        public BepEventHandlerContext(
            Project project,
            TaskId parentId,
            BazelTaskEventsHandler taskEventsHandler,
            DiagnosticsService diagnosticsService,
            BazelPathsResolver bazelPathsResolver
        ) {
            myProject = project;
            myParentId = parentId;
            myTaskEventsHandler = taskEventsHandler;
            myDiagnosticsService = diagnosticsService;
            myBazelPathsResolver = bazelPathsResolver;
        }

        public Project getProject() {
            return myProject;
        }

        public TaskId getParentId() {
            return myParentId;
        }

        public BazelTaskEventsHandler getTaskEventsHandler() {
            return myTaskEventsHandler;
        }

        public DiagnosticsService getDiagnosticsService() {
            return myDiagnosticsService;
        }

        public BazelPathsResolver getBazelPathsResolver() {
            return myBazelPathsResolver;
        }
    }

    /**
     * @return `true` if the event was handled and {@link #handleEvent} shouldn't be called for other handlers or for {@link BepServer}
     */
    boolean handleEvent(BuildEventStreamProtos.BuildEvent event);
}
