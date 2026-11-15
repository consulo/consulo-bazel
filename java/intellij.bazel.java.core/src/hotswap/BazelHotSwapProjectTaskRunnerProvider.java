package org.jetbrains.bazel.hotswap;

import com.intellij.java.debugger.impl.DebuggerSession;
import com.intellij.java.debugger.impl.ui.HotSwapUI;
import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompileContext;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.buildTask.AdditionalProjectTask;
import org.jetbrains.bazel.buildTask.AdditionalProjectTaskRunnerProvider;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class BazelHotSwapProjectTaskRunnerProvider implements AdditionalProjectTaskRunnerProvider {
    @Override
    public @Nullable AdditionalProjectTask createTask(Project project, CompileContext projectTaskContext, List<Label> targetsToBuild) {
        if (!HotSwapUtils.isHotSwapEligible(project)) {
            return null;
        }
        return new HotSwapTask(project, projectTaskContext, targetsToBuild);
    }
}

record HotSwapState(
    List<Path> jars,
    // TODO consulo: com.intellij.debugger.ui.HotSwapStatusListener is missing in Consulo
    // val listener: HotSwapStatusListener?,
    JarFileManifest oldManifest,
    List<DebuggerSession> sessions
) {
}

class HotSwapTask implements AdditionalProjectTask {
    private final Project myProject;
    private final CompileContext myProjectTaskContext;
    private final List<Label> myTargetsToBuild;

    private @Nullable HotSwapState myHotSwapState = null;

    HotSwapTask(
        Project project,
        CompileContext projectTaskContext,
        List<Label> targetsToBuild
    ) {
        myProject = project;
        myProjectTaskContext = projectTaskContext;
        myTargetsToBuild = targetsToBuild;
    }

    @Override
    public CompletableFuture<?> preRun() {
        List<Path> jars = getTargetJars();

        // A workaround to get hotswap callback so we may report hotswap status back
        // even if we disable built-in hotswap (built for JPS compiler)
        // TODO consulo: HotSwapUIImpl.HOT_SWAP_CALLBACK_KEY / HotSwapStatusListener are missing in Consulo
        // val listener = projectTaskContext.getUserData(HotSwapUIImpl.HOT_SWAP_CALLBACK_KEY)
        // This key disables built-in IDEA hotswap (built for JPS compiler)
        // consulo: upstream projectTaskContext.putUserData(HotSwapUIImpl.SKIP_HOT_SWAP_KEY, true); Consulo skips the built-in hotswap
        // after the current compilation through HotSwapUI
        HotSwapUI.getInstance(myProject).dontPerformHotswapAfterThisCompilation();

        List<DebuggerSession> sessions = BazelHotSwapManager.getInstance(myProject).getCurrentDebugSessions();
        if (sessions.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        JarFileManifest oldManifest = JarFileManifest.build(jars, /* previousManifest = */ null);
        myHotSwapState = new HotSwapState(jars, oldManifest, sessions);
        return CompletableFuture.completedFuture(null);
    }

    private List<Path> getTargetJars() {
        Set<URI> uris = new LinkedHashSet<>();
        for (Label it : myTargetsToBuild) {
            for (Module module : myProject.getInstance(ModuleTargetService.class).findModulesByLabel(/* label = */ it)) {
                JvmBinaryJarsEntity jvmBinaryJarsEntity = JvmBinaryJarsEntity.getJvmBinaryJarsEntity(module);
                if (jvmBinaryJarsEntity == null) {
                    continue;
                }
                for (String url : jvmBinaryJarsEntity.jars()) {
                    uris.add(URI.create(url));  // Don't use VFS here as we're dealing with files in bazel-out
                }
            }
        }
        List<Path> result = new ArrayList<>();
        for (URI uri : uris) {
            try {
                result.add(Path.of(uri).toRealPath());
            }
            catch (Exception ignored) {
                // consulo: upstream runCatching { it.toRealPath() }.getOrNull()
            }
        }
        return result;
    }

    @Override
    public CompletableFuture<?> postRun(BazelStatus result) {
        HotSwapState hotSwapState = myHotSwapState;
        if (hotSwapState == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (result != BazelStatus.SUCCESS) {
            // TODO consulo: HotSwapStatusListener is missing in Consulo
            // hotSwapState.listener?.onFailure(hotSwapState.sessions)
            return CompletableFuture.completedFuture(null);
        }
        JarFileManifest newManifest = JarFileManifest.build(hotSwapState.jars(), /* previousManifest = */ hotSwapState.oldManifest());
        return BazelHotSwapManager.getInstance(myProject).hotswap(
            new BazelHotSwapManager.HotSwapEnvironment(
                /* oldManifest = */ hotSwapState.oldManifest(),
                /* newManifest = */ newManifest,
                /* sessions = */ hotSwapState.sessions(),
                // TODO consulo: ProjectTaskContext.isAutoRun has no CompileContext counterpart
                /* isAutoRun = */ false
            )
        );
    }
}
