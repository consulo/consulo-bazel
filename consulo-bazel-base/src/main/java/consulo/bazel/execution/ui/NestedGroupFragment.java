/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.execution.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Consulo replacement of IntelliJ {@code com.intellij.execution.ui.NestedGroupFragment}: a fragment grouping child fragments
 * under a titled separator.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class NestedGroupFragment<S> extends SettingsEditorFragment<S, JComponent> {
    private final Supplier<List<SettingsEditorFragment<S, ?>>> myChildren = LazyValue.notNull(() -> {
        List<SettingsEditorFragment<S, ?>> children = createChildren();
        for (SettingsEditorFragment<S, ?> child : children) {
            Disposer.register(this, child);
            child.addSettingsEditorListener(editor -> fireEditorStateChanged());
        }
        return children;
    });

    private boolean myGroupComponentCreated;

    protected NestedGroupFragment(String id, LocalizeValue name, @Nullable LocalizeValue group, Predicate<? super S> initialSelection) {
        // consulo: upstream passes no component and creates it from the children, here the panel is filled on first use
        super(id, name, group, new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 5, true, false)), (s, c) -> {
        }, (s, c) -> {
        }, initialSelection);
    }

    protected abstract List<SettingsEditorFragment<S, ?>> createChildren();

    @Override
    public final List<SettingsEditorFragment<S, ?>> getChildren() {
        return myChildren.get();
    }

    public LocalizeValue getChildrenGroupName() {
        return getName();
    }

    @Override
    public JComponent component() {
        if (!myGroupComponentCreated) {
            myGroupComponentCreated = true;
            LocalizeValue title = getGroup().isEmpty() ? getName() : getGroup();
            myComponent.add(new TitledSeparator(title.get()));
            for (SettingsEditorFragment<S, ?> child : getChildren()) {
                myComponent.add(RunConfigurationFragmentedEditor.createRow(child));
            }
        }
        return myComponent;
    }

    @Override
    public boolean isInitiallyVisible(S s) {
        if (super.isInitiallyVisible(s)) {
            return true;
        }
        for (SettingsEditorFragment<S, ?> fragment : getChildren()) {
            if (fragment.isInitiallyVisible(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        for (SettingsEditorFragment<S, ?> fragment : getChildren()) {
            if (fragment.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void resetEditorFrom(S s) {
        for (SettingsEditorFragment<S, ?> child : getChildren()) {
            child.resetFrom(s);
        }
    }

    @Override
    protected void applyEditorTo(S s) throws ConfigurationException {
        for (SettingsEditorFragment<S, ?> child : getChildren()) {
            child.applyTo(s);
        }
    }
}
