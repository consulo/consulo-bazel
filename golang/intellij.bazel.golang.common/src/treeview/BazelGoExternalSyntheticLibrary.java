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
package org.jetbrains.bazel.golang.treeview;

import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a {@code SyntheticLibrary} with a mutable set of child files.
 */
// TODO consulo: IntelliJ SyntheticLibrary is missing, the class keeps its data and presentation
public class BazelGoExternalSyntheticLibrary implements ItemPresentation {
    private final String myPresentableText;
    private final SortedSet<VirtualFile> myChildFiles;

    public BazelGoExternalSyntheticLibrary(String presentableText, Collection<VirtualFile> childFiles) {
        myPresentableText = presentableText;
        myChildFiles = new TreeSet<>(Comparator.comparing(VirtualFile::toString));
        myChildFiles.addAll(childFiles);
    }

    public void addFiles(Collection<VirtualFile> files) {
        myChildFiles.addAll(files);
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    public SortedSet<VirtualFile> getSourceRoots() {
        return myChildFiles;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof BazelGoExternalSyntheticLibrary other &&
            other.myPresentableText.equals(myPresentableText) &&
            other.getSourceRoots().equals(getSourceRoots());
    }

    @Override
    public int hashCode() {
        return myPresentableText.hashCode();
    }

    @Override
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(myPresentableText);
    }

    // consulo: ItemPresentation of Consulo also has the location string
    @Override
    public @Nullable String getLocationString() {
        return null;
    }
}
