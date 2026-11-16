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
import com.goide.psi.impl.GoPackage;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.content.FileIndexFacade;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Wrapper around {@link BazelGoPackageEntity} that implements {@code GoPackage} from the Go plugin.
 * Differences from vanilla {@code GoPackage}:
 * - Same directory may appear in different {@link BazelGoPackage}s.
 * - {@link #files} will not necessarily return all files under {@code GoPackage.getDirectories}.
 * - {@link #navigate}s to the corresponding go rule instead of a directory.
 */
public class BazelGoPackage extends GoPackage {
    private final Project myProject;
    private final BazelGoPackageEntity myEntity;

    @RequiredReadAction
    public BazelGoPackage(
        Project project,
        BazelGoPackageEntity entity
    ) {
        super(
            project,
            getPackageName(project, sources(entity), entity.importPath()),
            sources(entity).stream()
                .map(VirtualFile::getParent)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(VirtualFile[]::new)
        );
        myProject = project;
        myEntity = entity;
    }

    /**
     * Returns a list of resolve targets, one for each import path component.
     * <p>
     * The final path component may either be the target name, or the containing directory name (in
     * which case the target name is usually `go_default_library`).
     * <p>
     * For instance, consider the target `//foo:bar` with import path `"github.com/user/foo/bar"`:
     *
     * <pre>
     * import "github.com/user/foo/bar"
     *         (1)        (2)  (3) (4)
     * </pre>
     * <p>
     * 1. `github.com` resolves to nothing, falls back to target `//foo:bar`.
     * 2. `user` resolves to nothing, falls back to target `//foo:bar`.
     * 3. `foo` resolves to the directory `foo/`.
     * 4. `bar` resolves to the target `//foo:bar`.
     * <p>
     * Another example would be the target `//one/two:go_default_library` with import path `"github.com/user/one/two"`:
     *
     * <pre>
     * import "github.com/user/one/two"
     *         (1)        (2)  (3) (4)
     * </pre>
     * <p>
     * 1. `github.com` resolves to nothing, falls back to directory `one/two/`.
     * 2. `user` resolves to nothing, falls back to directory `one/two/`.
     * 3. `one` resolves to the directory `one/`.
     * 4. `two` resolves to the directory `one/two/`.
     */
    @RequiredReadAction
    public PsiElement @Nullable [] getImportReferences() {
        PsiElement navigable = getNavigableElement();
        if (navigable == null) {
            return null;
        }
        Label mainLabel = getMainLabel();
        if (mainLabel == null) {
            return null;
        }
        return getImportReferences(mainLabel, navigable, myEntity.importPath());
    }

    /**
     * Resolves the import references for a given `Label`, `PsiElement`, and import path.
     *
     * @param label        the Bazel label associated with the import
     * @param buildElement the PsiElement (build file or directory) for which references are resolved
     * @param importPath   the import path string to resolve
     * @return an array of resolved {@link PsiElement}s corresponding to the components of the import path
     */
    @RequiredReadAction
    public static PsiElement[] getImportReferences(
        Label label,
        PsiElement buildElement,
        String importPath
    ) {
        List<String> pathComponents = Arrays.asList(importPath.split("/", -1));
        PsiElement[] importReferences = new PsiElement[pathComponents.size()];
        Arrays.fill(importReferences, buildElement);

        if (pathComponents.isEmpty()) {
            return importReferences;
        }

        // Get the last element (e.g., `bar` for the import path ending in `github.com/user/foo/bar`)
        String lastPathComponent = pathComponents.get(pathComponents.size() - 1);
        PsiElement lastElement = getLastElement(lastPathComponent, label, buildElement);
        if (lastElement == null) {
            return importReferences;
        }

        importReferences[pathComponents.size() - 1] = lastElement;
        PsiDirectory currentElement;
        if (lastElement instanceof PsiDirectory lastDirectory) {
            currentElement = lastDirectory.getParent();
        }
        else {
            PsiFile containingFile = lastElement.getContainingFile();
            PsiDirectory containingPackage = containingFile != null ? containingFile.getParent() : null;
            if (containingPackage != null && containingPackage.getName().equals(lastPathComponent)) {
                // Package name is the same as last component -- go one level up again
                currentElement = containingPackage.getParent();
            }
            else {
                currentElement = containingPackage;
            }
        }

        // Iterate backwards through the path components, linking names to the proper directories or elements
        for (int i = pathComponents.size() - 2; i >= 0; i--) {
            if (currentElement == null) {
                break;
            }

            String name = currentElement.getName();
            String pathComponent = pathComponents.get(i);
            if (name.equals(pathComponent)) {
                importReferences[i] = currentElement;
                currentElement = currentElement.getParent();
            }
            else {
                break;
            }
        }

        return importReferences;
    }

    /**
     * Finds the last element of the import path.
     *
     * @param name         the name of the final component in the import path
     * @param label        the Bazel label
     * @param buildElement the PsiElement representing the build file or directory
     * @return the corresponding resolved {@link PsiElement}, or `null` if it cannot be found
     */
    @RequiredReadAction
    private static @Nullable PsiElement getLastElement(
        String name,
        Label label,
        PsiElement buildElement
    ) {
        if (buildElement instanceof StarlarkCallExpression) {
            if (name.equals(label.getTargetName())) {
                return buildElement;
            }
            else {
                PsiFile containingFile = buildElement.getContainingFile();
                PsiDirectory parent = containingFile != null ? containingFile.getParent() : null;
                return parent != null && parent.getName().equals(name) ? parent : null;
            }
        }
        else if (buildElement instanceof StarlarkFile starlarkFile) {
            if (!starlarkFile.isBuildFile()) {
                return null;
            }
            PsiDirectory parent = starlarkFile.getParent();
            return parent != null && parent.getName().equals(name) ? parent : starlarkFile;
        }
        else {
            return null;
        }
    }

    /**
     * Package name is determined by package declaration in the source files.
     * By convention, this is *usually* equal to the last component of the `importpath`.
     * However, e.g., for `go_binary` the package is always `main`.
     * <p>
     * `rules_go` in principle allows any package name, but enforces that among packages passed to the linker,
     * each `importpath` corresponds to exactly one package name.
     * Very conveniently (see doc {@link BazelGoPackageEntity}), {@link BazelGoPackage} contains files with only one `importpath`,
     * so checking the package name for just one source file, not for all sources, is enough here.
     */
    @RequiredReadAction
    private static String getPackageName(
        Project project,
        List<VirtualFile> sources,
        String importPath
    ) {
        PsiManager psiManager = PsiManager.getInstance(project);
        GoFile goFile = sources.stream()
            .map(source -> psiManager.findFile(source) instanceof GoFile file ? file : null)
            .filter(Objects::nonNull)
            .filter(it -> !"ignore".equals(it.getBuildFlags()))
            .findFirst()
            .orElse(null);
        if (goFile != null) {
            // consulo: GoPackageUtil.getTrimmedPackageName(file) -> GoFile.getCanonicalPackageName() (the package name without `_test`)
            String packageName = goFile.getCanonicalPackageName();
            return packageName != null ? packageName : substringAfterLast(importPath);
        }
        return substringAfterLast(importPath);
    }

    // consulo: Kotlin String.substringAfterLast('/')
    private static String substringAfterLast(String importPath) {
        return importPath.substring(importPath.lastIndexOf('/') + 1);
    }

    // consulo: upstream private extension `BazelGoPackageEntity.sources()`; the sources are VFS urls
    private static List<VirtualFile> sources(BazelGoPackageEntity receiver) {
        List<VirtualFile> result = new ArrayList<>();
        for (String it : receiver.sources()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(it);
            if (file != null) {
                result.add(file);
            }
        }
        return result;
    }

    @Override
    @RequiredReadAction
    public Collection<PsiFile> files() {
        PsiManager psiManager = PsiManager.getInstance(myProject);
        return sources(myEntity).stream()
            .map(psiManager::findFile)
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @RequiredReadAction
    // consulo: com.intellij.util.Processor -> Predicate
    public boolean processFiles(Predicate<? super PsiFile> processor, Predicate<VirtualFile> virtualFileFilter) {
        if (!isValid()) {
            return true;
        }
        FileIndexFacade fileIndexFacade = FileIndexFacade.getInstance(myProject);
        for (PsiFile file : files()) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null && virtualFile.isValid() &&
                virtualFileFilter.test(virtualFile) &&
                !fileIndexFacade.isExcludedFile(virtualFile) &&
                !processor.test(file)
            ) {
                return false;
            }
        }
        return true;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        PsiElement navigable = getNavigableElement();
        if (navigable instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(requestFocus);
        }
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return getNavigableElement() instanceof Navigatable;
    }

    @Override
    /**
     * navigates to the target in the BUILD file or just the BUILD file
     */
    @RequiredReadAction
    public @Nullable PsiElement getNavigableElement() {
        Label mainLabel = getMainLabel();
        if (mainLabel == null) {
            return null;
        }
        return LabelResolveUtils.resolveLabel(myProject, mainLabel);
    }

    /**
     * A package can actually correspond to several labels in case of `embed`.
     * Unfortunately, {@link #getNavigableElement} allows for only one option, so we just take the first option.
     */
    private @Nullable Label getMainLabel() {
        TargetStorage targetUtils = TargetStorage.getTargetStorage(myProject);
        // consulo: upstream reads the referrers of the entity in the workspace model
        List<Label> labels = GoPackageEntities.getInstance(myProject).referrers(myEntity.getSymbolicId())
            .map(it -> it.getTargetKey().label())
            .toList();
        // A package may have a library target, and a test target that tests that library.
        // Don't resolve to a test target if we can.
        // TODO: migrate to full compound key
        for (Label it : labels) {
            BuildTarget targetSummary = targetUtils.getTargetSummary(it);
            if (targetSummary != null && targetSummary.getKind().ruleType() == RuleType.LIBRARY) {
                return it;
            }
        }
        return labels.isEmpty() ? null : labels.get(0);
    }

    @Override
    public String getCanonicalImport(@Nullable Module contextModule) {
        return myEntity.importPath();
    }

    @Override
    public String getImportPath(boolean withVendoring) {
        return myEntity.importPath();
    }

    @Override
    public List<String> getAllImportPaths(boolean withVendoring) {
        return List.of(myEntity.importPath());
    }

    @Override
    public String toString() {
        return "Package: " + myEntity.importPath();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BazelGoPackage that = (BazelGoPackage) o;
        return myEntity.equals(that.myEntity);
    }

    @Override
    public int hashCode() {
        return myEntity.hashCode();
    }
}
