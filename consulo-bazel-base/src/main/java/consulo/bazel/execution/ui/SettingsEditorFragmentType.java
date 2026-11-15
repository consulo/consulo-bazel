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

/**
 * Consulo replacement of IntelliJ {@code com.intellij.execution.ui.SettingsEditorFragmentType}: the kind of a
 * {@link SettingsEditorFragment}. The bridge {@link RunConfigurationFragmentedEditor} uses it only to lay out headers.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public enum SettingsEditorFragmentType {
    /**
     * Before run tasks. Not used by the bridge: Consulo shows the before run tasks itself.
     */
    BEFORE_RUN,
    /**
     * The header of a run configuration (e.g. "Run"), see {@link CommonParameterFragments#createHeader}.
     */
    HEADER,
    /**
     * Fragments that form the command line.
     */
    COMMAND_LINE,
    /**
     * Regular settings fragments.
     */
    EDITOR,
    /**
     * Check-box like option. Not used by the bridge (upstream {@code TagButton} is not ported).
     */
    TAG
}
