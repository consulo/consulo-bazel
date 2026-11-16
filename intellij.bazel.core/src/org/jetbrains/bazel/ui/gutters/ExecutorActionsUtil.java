package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.execution.action.Location;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionWithDelegate;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.ui.UIAction;
import org.jspecify.annotations.Nullable;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.ArrayList;
import java.util.List;

public final class ExecutorActionsUtil {
    private ExecutorActionsUtil() {
    }

    @RequiredReadAction
    public static List<AnAction> getExecutorActions(Project project, BuildTargetInfo target) {
        return getExecutorActions(new BazelRunLocation(project, target));
    }

    public static List<AnAction> getExecutorActions(Location<?> location) {
        List<AnAction> executorActions = ExecutorAction.getActionList();
        // consulo: upstream wraps each action in an anonymous LineMarkerActionWrapper whose dataSnapshot provides
        //   Location.DATA_KEY; Consulo's LineMarkerActionWrapper is internal (consulo.execution.impl), so LocationActionWrapper
        //   puts the location into the data context of the event
        List<AnAction> result = new ArrayList<>(executorActions.size());
        for (AnAction executorAction : executorActions) {
            result.add(new LocationActionWrapper(executorAction, location));
        }
        return result;
    }

    // consulo: see getExecutorActions; delegates like Consulo's internal LineMarkerActionWrapper
    private static final class LocationActionWrapper extends ActionGroup
        implements ActionWithDelegate<AnAction>, AnActionWithAsyncUpdate {
        private final AnAction myOrigin;
        private final Location<?> myLocation;

        LocationActionWrapper(AnAction origin, Location<?> location) {
            myOrigin = origin;
            myLocation = location;
            copyFrom(origin);
            if (!(origin instanceof ActionGroup)) {
                getTemplatePresentation().setPerformGroup(true);
                getTemplatePresentation().setPopupGroup(true);
            }
        }

        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            if (myOrigin instanceof ActionGroup group) {
                return group.getChildren(e == null ? null : wrapEvent(e));
            }
            return EMPTY_ARRAY;
        }

        @Override
        public boolean isDumbAware() {
            return myOrigin.isDumbAware();
        }

        @Override
        public boolean hideIfNoVisibleChildren() {
            return myOrigin instanceof ActionGroup group && group.hideIfNoVisibleChildren();
        }

        @Override
        public boolean disableIfNoVisibleChildren() {
            return !(myOrigin instanceof ActionGroup group) || group.disableIfNoVisibleChildren();
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            AnActionEvent wrapped = wrapEvent(e);
            if (myOrigin instanceof AnActionWithAsyncUpdate async) {
                return async.updateAsync(wrapped);
            }
            return Coroutine.first(UIAction.<Object, Object>apply(input -> {
                if (myOrigin instanceof AnActionWithSyncUpdate sync) {
                    sync.update(wrapped);
                }
                return input;
            }));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myOrigin.actionPerformed(wrapEvent(e));
        }

        @Override
        public AnAction getDelegate() {
            return myOrigin;
        }

        private AnActionEvent wrapEvent(AnActionEvent e) {
            DataContext dataContext = DataContext.builder()
                .parent(e.getDataContext())
                .add(Location.DATA_KEY, myLocation)
                .build();
            return new AnActionEvent(e.getInputEvent(), dataContext, e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        }
    }
}
