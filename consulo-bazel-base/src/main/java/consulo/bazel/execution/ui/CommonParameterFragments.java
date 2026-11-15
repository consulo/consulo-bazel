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

import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.TitledSeparator;

import java.awt.*;

/**
 * Consulo replacement of the helpers of IntelliJ {@code com.intellij.execution.ui.CommonParameterFragments} used by upstream
 * fragments.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class CommonParameterFragments {
    private CommonParameterFragments() {
    }

    public static void setMonospaced(Component field) {
        field.setFont(getMonospaced());
    }

    public static Font getMonospaced() {
        // consulo: upstream uses the editor font with the label font size
        return AWTLanguageEditorUtil.getEditorFont();
    }

    /**
     * A header fragment; upstream shows a bold label, the bridge shows a {@link TitledSeparator}.
     */
    public static <S> SettingsEditorFragment<S, TitledSeparator> createHeader(LocalizeValue title) {
        TitledSeparator separator = new TitledSeparator(title.get());
        SettingsEditorFragment<S, TitledSeparator> fragment = new SettingsEditorFragment<>(
            "title",
            LocalizeValue.empty(),
            null,
            separator,
            SettingsEditorFragmentType.HEADER,
            (settings, component) -> {
            },
            (settings, component) -> {
            },
            settings -> true
        );
        // consulo: upstream also calls setCanBeHidden(false), the bridge never hides fragments
        return fragment;
    }
}
