package org.jetbrains.bazel.ui.widgets.tool.window.components;

import org.jetbrains.bazel.ui.widgets.tool.window.components.FilterActionGroup.TargetFilter;

public class BazelTargetsPanelModel {
    // consulo: MutableSharedFlow<Unit> (replay = 1, DROP_OLDEST) of update requests -> a callback that requests an update
    private final Runnable myUpdateRequests;

    private boolean myMatchCase = false;

    private boolean myRegexMode = false;

    private volatile TargetFilter myTargetFilter = TargetFilter.OFF;

    private String mySearchQuery = "";

    private boolean myDisplayAsTree = true;

    public BazelTargetsPanelModel(Runnable updateRequests) {
        myUpdateRequests = updateRequests;
    }

    public boolean getMatchCase() {
        return myMatchCase;
    }

    public void setMatchCase(boolean value) {
        myMatchCase = value;
        updateTargets();
    }

    public boolean getRegexMode() {
        return myRegexMode;
    }

    public void setRegexMode(boolean value) {
        myRegexMode = value;
        updateTargets();
    }

    public TargetFilter getTargetFilter() {
        return myTargetFilter;
    }

    public void setTargetFilter(TargetFilter value) {
        myTargetFilter = value;
        updateTargets();
    }

    public String getSearchQuery() {
        return mySearchQuery;
    }

    public void setSearchQuery(String value) {
        mySearchQuery = value;
        updateTargets();
    }

    public boolean getDisplayAsTree() {
        return myDisplayAsTree;
    }

    public void setDisplayAsTree(boolean value) {
        myDisplayAsTree = value;
        updateTargets();
    }

    private void updateTargets() {
        // consulo: check(updateRequests.tryEmit(Unit)) - the request never fails
        myUpdateRequests.run();
    }
}
