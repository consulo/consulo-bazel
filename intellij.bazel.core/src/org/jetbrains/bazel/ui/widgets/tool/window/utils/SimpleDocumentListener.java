package org.jetbrains.bazel.ui.widgets.tool.window.utils;

import org.jspecify.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SimpleDocumentListener implements DocumentListener {
    private final Runnable myOnUpdate;

    public SimpleDocumentListener(Runnable onUpdate) {
        myOnUpdate = onUpdate;
    }

    @Override
    public void insertUpdate(@Nullable DocumentEvent e) {
        myOnUpdate.run();
    }

    @Override
    public void removeUpdate(@Nullable DocumentEvent e) {
        myOnUpdate.run();
    }

    @Override
    public void changedUpdate(@Nullable DocumentEvent e) {
        myOnUpdate.run();
    }
}
