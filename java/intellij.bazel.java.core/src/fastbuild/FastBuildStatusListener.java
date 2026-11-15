package org.jetbrains.bazel.fastbuild;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(ComponentScope.PROJECT)
public interface FastBuildStatusListener {
    void fastBuildStarted();

    void fastBuildFinished(FastBuildStatus fastBuildStatus);

    void fastBuildTargetStarted();

    void fastBuildTargetFinished(FastBuildStatus.FastBuildTargetStatus fastBuildTargetStatus);

    // consulo: Consulo topics are identified by the listener class
    Class<FastBuildStatusListener> TOPIC = FastBuildStatusListener.class;
}
