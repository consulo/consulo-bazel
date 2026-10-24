package org.jetbrains.bazel.sync.status;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

@TopicAPI(ComponentScope.PROJECT)
public interface SyncStatusListener {
    void syncStarted();

    void syncFinished(boolean canceled);

    // consulo: Consulo topics are identified by the listener class
    Class<SyncStatusListener> TOPIC = SyncStatusListener.class;
}
