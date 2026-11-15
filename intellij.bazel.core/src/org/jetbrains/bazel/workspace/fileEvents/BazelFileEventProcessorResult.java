package org.jetbrains.bazel.workspace.fileEvents;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Processing result. To be used in tests
 */
// consulo: upstream declares it in BazelFileEventProcessor.kt; own file to keep the FQN
public class BazelFileEventProcessorResult {
    public static final BazelFileEventProcessorResult EMPTY =
        new BazelFileEventProcessorResult(List.of(), List.of(), List.of());

    private final Collection<Path> myRemovedFromModel;
    private final Collection<Path> myAddedToModel;
    private final Collection<Path> myFailedToEvaluate;

    public BazelFileEventProcessorResult(
        Collection<Path> removedFromModel,
        Collection<Path> addedToModel,
        Collection<Path> failedToEvaluate
    ) {
        myRemovedFromModel = removedFromModel;
        myAddedToModel = addedToModel;
        myFailedToEvaluate = failedToEvaluate;
    }

    public Collection<Path> getRemovedFromModel() {
        return myRemovedFromModel;
    }

    public Collection<Path> getAddedToModel() {
        return myAddedToModel;
    }

    public Collection<Path> getFailedToEvaluate() {
        return myFailedToEvaluate;
    }

    public boolean isEmpty() {
        return myRemovedFromModel.isEmpty() && myAddedToModel.isEmpty() && myFailedToEvaluate.isEmpty();
    }
}
