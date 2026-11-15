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

import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jspecify.annotations.Nullable;

import java.util.List;

// TODO consulo: IntelliJ SyntheticLibrary (a library shown under External Libraries) is missing; the class keeps its data and
//  presentation, its roots are indexed through GoAdditionalLibraryRootsProvider
public record GoExternalSyntheticLibrary(List<VirtualFile> files) implements ItemPresentation {
    // consulo: upstream internal top-level const of GoExternalSyntheticLibrary.kt
    public static final String GO_EXTERNAL_LIBRARY_ROOT_NAME = "Go Libraries";

    public List<VirtualFile> getSourceRoots() {
        return files.stream().filter(VirtualFile::isValid).toList();
    }

    @Override
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(GO_EXTERNAL_LIBRARY_ROOT_NAME);
    }

    // consulo: ItemPresentation of Consulo also has the location string
    @Override
    public @Nullable String getLocationString() {
        return null;
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    /**
     * Prevent {@link #files} from polluting
     * {@code com.intellij.util.indexing.dependenciesCache.SyntheticLibraryDescriptor.debugLibraryName}
     */
    @Override
    public String toString() {
        return GO_EXTERNAL_LIBRARY_ROOT_NAME;
    }
}
