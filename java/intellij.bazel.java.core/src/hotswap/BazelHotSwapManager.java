package org.jetbrains.bazel.hotswap;

import com.intellij.java.debugger.impl.DebuggerSession;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages hotswapping for bazel java_binary run configurations.
 * <p>
 * Customers could override the default implementation to add their telemetry, see BAZEL-3091
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface BazelHotSwapManager {
    class HotSwapEnvironment {
        private final JarFileManifest myOldManifest;
        private final JarFileManifest myNewManifest;
        // TODO consulo: com.intellij.debugger.ui.HotSwapStatusListener (HotSwapUIImpl.HOT_SWAP_CALLBACK_KEY) is missing in Consulo
        // val listener: HotSwapStatusListener?,
        private final List<DebuggerSession> mySessions;
        private final boolean myIsAutoRun;

        public HotSwapEnvironment(
            JarFileManifest oldManifest,
            JarFileManifest newManifest,
            List<DebuggerSession> sessions,
            boolean isAutoRun
        ) {
            myOldManifest = oldManifest;
            myNewManifest = newManifest;
            mySessions = sessions;
            myIsAutoRun = isAutoRun;
        }

        public JarFileManifest getOldManifest() {
            return myOldManifest;
        }

        public JarFileManifest getNewManifest() {
            return myNewManifest;
        }

        public List<DebuggerSession> getSessions() {
            return mySessions;
        }

        public boolean isAutoRun() {
            return myIsAutoRun;
        }
    }

    CompletableFuture<?> hotswap(HotSwapEnvironment environment);

    List<DebuggerSession> getCurrentDebugSessions();

    static BazelHotSwapManager getInstance(Project project) {
        return project.getInstance(BazelHotSwapManager.class);
    }
}
