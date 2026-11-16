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
package org.jetbrains.bazel.dotnet.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor;
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor.WorkspaceTypeEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * consulo: Consulo-only, the .NET build target data persisted in the workspace snapshot (like {@code RustWorkspaceTypeContributor}).
 */
@ExtensionImpl
public class DotNetWorkspaceTypeContributor implements WorkspaceTypeContributor {
    @Override
    public List<WorkspaceTypeEntry> contribute(Project project) {
        List<WorkspaceTypeEntry> result = new ArrayList<>();
        WorkspaceTypeContributor.type(result, DotNetBuildTarget.class);
        return result;
    }
}
