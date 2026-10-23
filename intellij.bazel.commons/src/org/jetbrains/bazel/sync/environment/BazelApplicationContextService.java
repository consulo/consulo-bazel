package org.jetbrains.bazel.sync.environment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

@ServiceAPI(ComponentScope.APPLICATION)
public interface BazelApplicationContextService {
    default boolean getDisableFileWatcherSymlinkExclusion() {
        return false;
    }

    default boolean getDisableHardLinksOutputFiles() {
        return false;
    }

    default boolean getForceBazeliskDownload() {
        return false;
    }

    default boolean getEnablePty() {
        return true;
    }
}
