package org.jetbrains.bazel.startup.utils;

import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import java.util.concurrent.CompletableFuture;

// todo to be removed
public abstract class BazelProjectActivity implements PostStartupActivity {
    @Override
    public final void runActivity(Project project, UIAccess uiAccess) {
        if (org.jetbrains.bazel.config.project.isBazelProject(project)) {
            executeForBazelProject(project);
        }
    }

    public abstract CompletableFuture<?> executeForBazelProject(Project project);
}
