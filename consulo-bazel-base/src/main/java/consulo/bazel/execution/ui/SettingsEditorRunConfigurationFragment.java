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
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.function.Supplier;

/**
 * Consulo-only adapter: shows a whole {@link SettingsEditor} (e.g. a {@link RunConfigurationFragmentedEditor}) as one
 * non-removable Consulo {@link RunConfigurationFragment}. Replaces IntelliJ's {@code RunConfiguration.getConfigurationEditor()},
 * which Consulo does not have; a run configuration returns it from {@code RunConfiguration.createFragments()}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SettingsEditorRunConfigurationFragment<T> implements RunConfigurationFragment<T> {
    private final String myId;
    private final LocalizeValue myName;
    private final Supplier<? extends SettingsEditor<T>> myEditorFactory;

    public SettingsEditorRunConfigurationFragment(String id, LocalizeValue name, Supplier<? extends SettingsEditor<T>> editorFactory) {
        myId = id;
        myName = name;
        myEditorFactory = editorFactory;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        SettingsEditor<T> editor = myEditorFactory.get();
        Disposer.register(parent, editor);
        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(editor.getUIComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(T target) {
                editor.resetFrom(target);
            }

            @RequiredUIAccess
            @Override
            public void apply(T target) throws ConfigurationException {
                editor.applyTo(target);
            }
        };
    }
}
