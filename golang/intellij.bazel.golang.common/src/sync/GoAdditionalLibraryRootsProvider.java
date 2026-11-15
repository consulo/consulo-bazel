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
package org.jetbrains.bazel.golang.sync;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * consulo: IntelliJ {@code AdditionalLibraryRootsProvider} / {@code SyntheticLibrary} do not exist; the roots of the Go external
 * library are indexed with an {@link IndexableSetContributor} (TODO consulo: they are not shown under External Libraries).
 */
@ExtensionImpl
public class GoAdditionalLibraryRootsProvider extends IndexableSetContributor {
    public Collection<GoExternalSyntheticLibrary> getAdditionalProjectLibraries(Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return List.of();
        }
        GoExternalSyntheticLibrary library = GoExternalLibraryManager.getInstance(project).getLibrary();
        if (library != null) {
            return List.of(library);
        }
        else {
            return List.of();
        }
    }

    @Override
    public Set<VirtualFile> getAdditionalProjectRootsToIndex(Project project) {
        Set<VirtualFile> roots = new LinkedHashSet<>();
        for (GoExternalSyntheticLibrary library : getAdditionalProjectLibraries(project)) {
            roots.addAll(library.getSourceRoots());
        }
        return roots;
    }

    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
        return Set.of();
    }
}
