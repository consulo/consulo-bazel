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
package consulo.bazel.core.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * Upstream {@code <group id="Bazel.DiagnosticTools">} of {@code intellij.bazel.core.xml}; its parent {@code HelpDiagnosticTools}
 * does not exist in Consulo, the group is added to {@code HelpMenu}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ActionImpl(
    id = "Bazel.DiagnosticTools",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(id = "Bazel.ToggleBazelLogAction"),
        @ActionRef(id = "Bazel.ShowBazelLogAction")
    },
    parents = @ActionParentRef(value = @ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.LAST)
)
public class BazelDiagnosticToolsGroup extends DefaultActionGroup {
}
