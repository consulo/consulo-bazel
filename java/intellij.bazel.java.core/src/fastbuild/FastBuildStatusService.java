package org.jetbrains.bazel.fastbuild;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class FastBuildStatusService {
    private final Project myProject;

    @Inject
    public FastBuildStatusService(Project project) {
        myProject = project;
    }

    public void startFastBuild() {
        myProject.getMessageBus().syncPublisher(FastBuildStatusListener.TOPIC).fastBuildStarted();
    }

    public void finishFastBuild(FastBuildStatus fastBuildStatus) {
        myProject.getMessageBus().syncPublisher(FastBuildStatusListener.TOPIC).fastBuildFinished(fastBuildStatus);
    }

    public void startFastBuildTarget() {
        myProject.getMessageBus().syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetStarted();
    }

    public void finishFastBuildTarget(FastBuildStatus.FastBuildTargetStatus fastBuildTargetStatus) {
        myProject.getMessageBus().syncPublisher(FastBuildStatusListener.TOPIC).fastBuildTargetFinished(fastBuildTargetStatus);
    }

    public static FastBuildStatusService getInstance(Project project) {
        return project.getInstance(FastBuildStatusService.class);
    }
}
