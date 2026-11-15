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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.LabeledComponent;

import javax.swing.*;
import java.awt.*;

/**
 * Consulo replacement of IntelliJ
 * {@code com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent}:
 * a component of a {@link SettingsEditorFragment} with a label on the left.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class SettingsEditorLabeledComponent<C extends JComponent> extends LabeledComponent<C> {
    public SettingsEditorLabeledComponent(LocalizeValue label, C component) {
        setText(label.get());
        setLabelLocation(BorderLayout.WEST);
        setComponent(component);
    }
}
