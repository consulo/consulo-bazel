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

/**
 * Consulo replacement of IntelliJ {@code com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo}
 * (together with its super interface {@code SettingsFragmentInfo}): the description of a labeled fragment created by
 * {@link SettingsEditorFragmentContainer#addLabeledSettingsEditorFragment}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public interface LabeledSettingsFragmentInfo {
    LocalizeValue getEditorLabel();

    String getSettingsId();

    LocalizeValue getSettingsName();

    /**
     * Empty for no group.
     */
    LocalizeValue getSettingsGroup();

    default int getSettingsPriority() {
        return 0;
    }

    default SettingsEditorFragmentType getSettingsType() {
        return SettingsEditorFragmentType.EDITOR;
    }

    /**
     * Empty for no hint.
     */
    LocalizeValue getSettingsHint();

    /**
     * Empty for no action hint.
     */
    LocalizeValue getSettingsActionHint();
}
