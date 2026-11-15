package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.action.Location;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.List;

public final class ExecutorActionsUtil {
    private ExecutorActionsUtil() {
    }

    @RequiredReadAction
    public static List<AnAction> getExecutorActions(Project project, BuildTarget target) {
        return getExecutorActions(new BazelRunLocation(project, target));
    }

    public static List<AnAction> getExecutorActions(Location<?> location) {
        List<AnAction> executorActions = ExecutorAction.getActionList();
        // TODO consulo: LineMarkerActionWrapper is internal API (consulo.execution.impl) and Consulo actions have no
        //  dataSnapshot(DataSink); the executor actions are returned without providing the location as Location.DATA_KEY:
        //  return executorActions.map { executorAction ->
        //    object : LineMarkerActionWrapper(location.psiElement, executorAction) {
        //      override fun dataSnapshot(sink: DataSink) {
        //        sink[Location.DATA_KEY] = location
        //      }
        //    }
        //  }
        return executorActions;
    }
}
