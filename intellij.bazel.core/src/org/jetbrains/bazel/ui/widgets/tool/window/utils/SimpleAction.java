package org.jetbrains.bazel.ui.widgets.tool.window.utils;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

public class SimpleAction extends AnAction {
    private final Runnable myAction;

    public SimpleAction(Runnable action) {
        myAction = action;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        myAction.run();
    }
}
