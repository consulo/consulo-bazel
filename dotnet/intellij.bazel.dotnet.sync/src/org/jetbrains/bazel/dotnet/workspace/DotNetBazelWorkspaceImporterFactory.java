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
package org.jetbrains.bazel.dotnet.workspace;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.BazelWorkspaceImporterFactory;
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter.WorkspaceImporterContext;

// consulo: Consulo-only, like RustBazelWorkspaceImporterFactory (a top-level class: an @ExtensionImpl cannot be nested)
@ExtensionImpl
public class DotNetBazelWorkspaceImporterFactory implements BazelWorkspaceImporterFactory {
    @Override
    public BazelWorkspaceImporter createWorkspaceImporter(WorkspaceImporterContext context) {
        return new DotNetBazelWorkspaceImporter(context);
    }
}
