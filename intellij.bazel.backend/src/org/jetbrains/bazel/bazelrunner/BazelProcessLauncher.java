package org.jetbrains.bazel.bazelrunner;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import org.jetbrains.bazel.bazelrunner.BazelCommand.BazelCommandExecutionDescriptor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

public interface BazelProcessLauncher {
    Process launchProcess(BazelCommandExecutionDescriptor executionDescriptor);

    @ExtensionAPI(ComponentScope.APPLICATION)
    interface BazelProcessLauncherProvider {
        BazelProcessLauncher createBazelProcessLauncher(
            Path workspaceRoot,
            Map<String, String> parentEnvironment
        );

        static ExtensionPoint<BazelProcessLauncherProvider> getEp() {
            return Application.get().getExtensionPoint(BazelProcessLauncherProvider.class);
        }

        static BazelProcessLauncherProvider getInstance() {
            @Nullable BazelProcessLauncherProvider provider = getEp().findFirstSafe(it -> true);
            return provider != null ? provider : DefaultBazelProcessLauncherProvider.INSTANCE;
        }
    }
}
