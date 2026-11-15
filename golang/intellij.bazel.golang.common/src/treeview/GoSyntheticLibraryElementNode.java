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

import com.goide.GoIcons;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * Represents a Go directory node within the "External Libraries" project view.
 * <p>
 * It can have an arbitrary amount of child nodes, forming a tree. Child nodes may be added and
 * queried with the appropriate methods.
 */
// TODO consulo: IntelliJ SyntheticLibraryElementNode is missing; the node is a ProjectViewNode with the presentation and
//  the file containment of the library
public class GoSyntheticLibraryElementNode extends ProjectViewNode<BazelGoExternalSyntheticLibrary> {
    private final ItemPresentation myItemPresentation;
    private final Map<String, AbstractTreeNode<?>> myChildren;

    GoSyntheticLibraryElementNode(
        Project project,
        BazelGoExternalSyntheticLibrary library,
        String dirName,
        ViewSettings settings,
        SortedMap<String, AbstractTreeNode<?>> children
    ) {
        super(project, library, settings);
        myItemPresentation = new BazelGoLibraryItemPresentation(dirName);
        myChildren = children;
    }

    @Override
    public Collection<AbstractTreeNode<?>> getChildren() {
        return myChildren.values();
    }

    public @Nullable GoSyntheticLibraryElementNode getChildNode(String dirName) {
        return myChildren.get(dirName) instanceof GoSyntheticLibraryElementNode node ? node : null;
    }

    public void addChild(String dirName, AbstractTreeNode<?> child) {
        myChildren.put(dirName, child);
    }

    public boolean hasChild(String dirName) {
        return myChildren.containsKey(dirName);
    }

    public void addFiles(Collection<VirtualFile> files) {
        BazelGoExternalSyntheticLibrary library = getValue();
        if (library != null) {
            library.addFiles(files);
        }
    }

    // consulo: upstream SyntheticLibraryElementNode.contains(file): the file is under a source root of the library
    @Override
    public boolean contains(VirtualFile file) {
        BazelGoExternalSyntheticLibrary library = getValue();
        if (library == null) {
            return false;
        }
        for (VirtualFile root : library.getSourceRoots()) {
            if (VirtualFileUtil.isAncestor(root, file, false)) {
                return true;
            }
        }
        return false;
    }

    // consulo: upstream SyntheticLibraryElementNode.update(presentation) shows the item presentation
    @Override
    protected void update(PresentationData presentation) {
        presentation.updateFrom(myItemPresentation);
    }

    private static class BazelGoLibraryItemPresentation implements ItemPresentation {
        private final String myPresentableText;

        BazelGoLibraryItemPresentation(String presentableText) {
            myPresentableText = presentableText;
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

        @Override
        public Image getIcon() {
            return GoIcons.PACKAGE;
        }
    }
}
