/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.workspacePath;

import org.jetbrains.bazel.commons.WorkspacePath;
import org.jetbrains.bazel.workspace.WorkspaceRoot;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Uses the package path locations to resolve a workspace path.
 */
public record WorkspacePathResolverImpl(WorkspaceRoot workspaceRoot) implements WorkspacePathResolver {
    @Override
    public List<Path> resolveToIncludeDirectories(WorkspacePath relativePath) {
        return List.of(workspaceRoot.fileForPath(relativePath));
    }

    @Override
    public Path findPackageRoot(String relativePath) {
        return workspaceRoot.directory();
    }

    @Override
    public @Nullable WorkspacePath getWorkspacePath(Path absolutePath) {
        return workspaceRoot.workspacePathForSafe(absolutePath);
    }

    @Override
    public @Nullable WorkspaceRoot findWorkspaceRoot(Path absolutePath) {
        return workspaceRoot.isInWorkspace(absolutePath) ? workspaceRoot : null;
    }
}
