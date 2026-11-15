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

import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.ui.ex.awt.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Consulo replacement of the static helpers of IntelliJ {@code com.intellij.execution.ui.CommandLinePanel} used by upstream
 * fragments (the panel itself is not ported).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class CommandLinePanel {
    private CommandLinePanel() {
    }

    public static Dimension setMinimumWidth(Component component, int width) {
        Dimension size = new Dimension(width, Math.max(JBUI.scale(30), component.getMinimumSize().height));
        component.setMinimumSize(size);
        if (component instanceof RawCommandLineEditor rawCommandLineEditor
            && rawCommandLineEditor.getTextField() instanceof JTextField textField) {
            textField.setColumns(0);
        }
        return size;
    }
}
