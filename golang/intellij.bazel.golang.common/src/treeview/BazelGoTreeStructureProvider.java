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
package org.jetbrains.bazel.golang.treeview;

import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.PsiFileNode;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.golang.sync.GoExternalSyntheticLibrary;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Modifies the project view by replacing the External Go Libraries root node (containing a flat
 * list of sources) with a root node that structures sources based on their import paths.
 */
// TODO consulo: not registered (upstream treeStructureProvider, order="last"). Without IntelliJ SyntheticLibrary there is no
//  "Go Libraries" node under External Libraries to restructure, see GoExternalSyntheticLibrary
@SuppressWarnings({"rawtypes", "unchecked"})
public class BazelGoTreeStructureProvider implements TreeStructureProvider, DumbAware {
    @Override
    public Collection<AbstractTreeNode> modify(
        AbstractTreeNode parent,
        Collection<AbstractTreeNode> children,
        ViewSettings settings
    ) {
        Project project = Objects.requireNonNull(parent.getProject());
        if (!org.jetbrains.bazel.config.project.isBazelProject(project) ||
            !isGoBazelExternalLibraryRoot(parent)
        ) {
            return children;
        }
        Map<VirtualFile, AbstractTreeNode<?>> originalFileNodes = getOriginalFileNodesMap(children);

        TreeMap<String, AbstractTreeNode<?>> newChildren = new TreeMap<>();

        // consulo: upstream reads the BazelGoPackageEntity entities of the workspace model
        for (BazelGoPackageEntity goPackage : GoPackageEntities.getInstance(project).getPackages()) {
            if (goPackage.importPath().isEmpty()) {
                continue;
            }
            generateTree(
                settings,
                project,
                newChildren,
                goPackage.importPath(),
                goPackage.sources().stream()
                    .map(it -> VirtualFileManager.getInstance().findFileByUrl(it))
                    .filter(Objects::nonNull)
                    .toList(),
                originalFileNodes
            );
        }
        return (Collection) newChildren.values();
    }

    // consulo: upstream private top-level functions of BazelGoTreeStructureProvider.kt

    private static void generateTree(
        ViewSettings settings,
        Project project,
        SortedMap<String, AbstractTreeNode<?>> newChildren,
        String importPath,
        Collection<VirtualFile> availableFiles,
        Map<VirtualFile, AbstractTreeNode<?>> originalFileNodes
    ) {
        Iterator<Path> pathIter = Paths.get(importPath).iterator();
        if (!pathIter.hasNext()) {
            return;
        }
        // Root nodes (e.g., src) have to be added directly to newChildren.
        String rootName = pathIter.next().toString();
        GoSyntheticLibraryElementNode root =
            (GoSyntheticLibraryElementNode) newChildren.computeIfAbsent(
                rootName,
                unused ->
                    new GoSyntheticLibraryElementNode(
                        project,
                        new BazelGoExternalSyntheticLibrary(rootName, availableFiles),
                        rootName,
                        settings,
                        new TreeMap<>()
                    )
            );

        // Child nodes (e.g., package_name under src) are added under root nodes recursively.
        GoSyntheticLibraryElementNode leaf =
            buildChildTree(settings, project, availableFiles, pathIter, root);

        // Required for files to actually show up in the Project View.
        for (VirtualFile file : availableFiles) {
            AbstractTreeNode<?> node = originalFileNodes.get(file);
            if (node != null) {
                leaf.addChild(file.getName(), node);
            }
        }
    }

    /**
     * Recurse down the import path tree and add elements as children.
     * <p>
     * <p>
     * Fills previously created nodes with source files from the current import path.
     */
    private static GoSyntheticLibraryElementNode buildChildTree(
        ViewSettings settings,
        Project project,
        Collection<VirtualFile> files,
        Iterator<Path> pathIter,
        GoSyntheticLibraryElementNode parentNode
    ) {
        GoSyntheticLibraryElementNode parent = parentNode;
        while (pathIter.hasNext()) {
            parent.addFiles(files);
            String dirName = pathIter.next().toString();

            // current path already was created, no need to re-create
            if (parent.hasChild(dirName)) {
                GoSyntheticLibraryElementNode childNode = parent.getChildNode(dirName);
                if (childNode != null) {
                    parent = childNode;
                }
                continue;
            }
            GoSyntheticLibraryElementNode libraryNode =
                new GoSyntheticLibraryElementNode(
                    project,
                    new BazelGoExternalSyntheticLibrary(dirName, files),
                    dirName,
                    settings,
                    new TreeMap<>()
                );
            parent.addChild(dirName, libraryNode);
            parent = libraryNode;
        }
        return parent;
    }

    private static boolean isGoBazelExternalLibraryRoot(AbstractTreeNode<?> parent) {
        String name = parent.getName();
        if (name == null) {
            return false;
        }
        return name.equals(GoExternalSyntheticLibrary.GO_EXTERNAL_LIBRARY_ROOT_NAME);
    }

    private static Map<VirtualFile, AbstractTreeNode<?>> getOriginalFileNodesMap(Collection<AbstractTreeNode> children) {
        Map<VirtualFile, AbstractTreeNode<?>> result = new LinkedHashMap<>();
        List<AbstractTreeNode> nodes = new ArrayList<>(children);
        for (AbstractTreeNode node : nodes) {
            if (node instanceof PsiFileNode fileNode) {
                VirtualFile virtualFile = fileNode.getVirtualFile();
                if (virtualFile != null) {
                    result.put(virtualFile, fileNode);
                }
            }
        }
        return result;
    }
}
