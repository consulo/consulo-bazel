/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.golang.resolve;

import com.goide.psi.GoFile;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream com.goide packageFactory, order="first"). The GoPackageFactory extension point and
//  GoPackage are missing in consulo-google-go
public class BazelGoPackageFactory /* : GoPackageFactory */ {
    public @Nullable BazelGoPackage createPackage(GoFile goFile) {
        Project project = goFile.getProject();
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return null;
        }
        BazelGoPackageEntity goPackageEntity = getGoPackageEntity(goFile);
        if (goPackageEntity == null) {
            return null;
        }
        return new BazelGoPackage(project, goPackageEntity);
    }

    public @Nullable BazelGoPackage createPackage(String packageName, PsiDirectory... directories) {
        return null;
    }

    // consulo: upstream internal top-level function of BazelGoPackageFactory.kt
    public static @Nullable BazelGoPackageEntity getGoPackageEntity(GoFile goFile) {
        Project project = goFile.getProject();
        VirtualFile virtualFile = goFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        // `BazelGoPackageEntity` is already part of VFU index due to `sources` property
        // consulo: the workspace model virtual file url index is replaced by the source url index of GoPackageEntities
        return GoPackageEntities.getInstance(project).findBySourceUrl(virtualFile.getUrl());
    }
}
