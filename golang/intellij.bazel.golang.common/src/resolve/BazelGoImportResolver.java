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

import com.goide.psi.impl.imports.GoImportReference;
import com.goide.psi.impl.imports.GoImportResolver;
import consulo.annotation.component.ExtensionImpl;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.idea.codeInsight.navigation.CtrlMouseHandler;
import consulo.language.editor.documentation.DocumentationProviderEx;
import consulo.language.impl.psi.SyntheticFileSystemItem;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.ImportPathId;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

// consulo: upstream id="Bazel.ImportResolver", order="first"; the order is restricted to the plugin owning the extension point
@ExtensionImpl
public class BazelGoImportResolver implements GoImportResolver {
    @Override
    @RequiredReadAction
    public @Nullable Collection<BazelGoPackage> resolve(
        String importPath,
        Project project,
        @Nullable Module module,
        @Nullable ResolveState resolveState
    ) {
        if (!supportsGo(project)) {
            return null;
        }
        BazelGoPackage goPackage = resolveGoPackage(project, importPath);
        return goPackage != null ? List.of(goPackage) : null;
    }

    @Override
    public ThreeState supportsRelativeImportPaths(Project project, @Nullable Module module) {
        if (!supportsGo(project)) {
            return ThreeState.UNSURE;
        }
        return ThreeState.NO;
    }

    @Override
    public ThreeState supportsInternalPackages(Project project, @Nullable Module module) {
        if (!supportsGo(project)) {
            return ThreeState.UNSURE;
        }
        return ThreeState.NO;
    }

    @Override
    public ThreeState supportsSdkInternalPackages(Project project, @Nullable Module module) {
        if (!supportsGo(project)) {
            return ThreeState.UNSURE;
        }
        return ThreeState.NO;
    }

    @Override
    @RequiredReadAction
    public ResolveResult @Nullable [] resolve(GoImportReference reference) {
        Project project = reference.getElement().getProject();
        if (!supportsGo(project)) {
            return null;
        }
        String importPath = reference.getFileReferenceSet().getPathString();
        BazelGoPackage goPackage = resolveGoPackage(project, importPath);
        if (goPackage == null) {
            return null;
        }
        return doResolve(goPackage, reference.getIndex());
    }

    private static boolean supportsGo(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    // consulo: upstream private top-level functions of BazelGoImportResolver.kt

    @RequiredReadAction
    private static @Nullable BazelGoPackage resolveGoPackage(Project project, String importPath) {
        // consulo: upstream resolves the ImportPathId symbolic id in the workspace model
        BazelGoPackageEntity entity = GoPackageEntities.getInstance(project).resolve(new ImportPathId(importPath));
        if (entity == null) {
            return null;
        }
        return new BazelGoPackage(project, entity);
    }

    @RequiredReadAction
    private static ResolveResult[] doResolve(BazelGoPackage goPackage, int index) {
        return List.of(goPackage)
            .stream()
            .map(BazelGoPackage::getImportReferences)
            .filter(Objects::nonNull)
            .filter(it -> index < it.length)
            .map(it -> it[index])
            .filter(Objects::nonNull)
            .map(GoPackageFileSystemItem::getInstance)
            .filter(Objects::nonNull)
            .map(PsiElementResolveResult::new)
            .toArray(ResolveResult[]::new);
    }
}

/**
 * {@link GoImportReference} must resolve to a {@link PsiFileSystemItem}, but we might want it to
 * resolve to a build rule in a {@code BuildFile}. We'll just return the {@code BuildFile} with a
 * navigation redirect.
 */
class GoPackageFileSystemItem extends SyntheticFileSystemItem {
    private final String myName;
    private final StarlarkCallExpression myRule;

    private GoPackageFileSystemItem(String name, StarlarkCallExpression rule) {
        super(rule.getProject());
        myName = name;
        myRule = rule;
    }

    @RequiredReadAction
    static @Nullable PsiFileSystemItem getInstance(PsiElement element) {
        if (element instanceof PsiFileSystemItem fileSystemItem) {
            return fileSystemItem;
        }
        else if (element instanceof StarlarkCallExpression callExpression) {
            String calledFunctionName = callExpression.getCalledFunctionName();
            return calledFunctionName != null ? new GoPackageFileSystemItem(calledFunctionName, callExpression) : null;
        }
        else {
            return null;
        }
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PsiElement getNavigationElement() {
        return myRule;
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiFileSystemItem getParent() {
        PsiFile containingFile = myRule.getContainingFile();
        if (containingFile instanceof StarlarkFile starlarkFile && starlarkFile.isBuildFile()) {
            PsiDirectory parent = starlarkFile.getParent();
            return parent;
        }
        return null;
    }

    @Override
    public @Nullable VirtualFile getVirtualFile() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isPhysical() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean processChildren(PsiElementProcessor<PsiFileSystemItem> psiElementProcessor) {
        return false;
    }
}

/**
 * Redirects quick navigation text on the fake file system item back to the build rule.
 */
// TODO consulo: not registered (upstream documentationProvider): Consulo documentation providers are language bound, and the
//  fake file system item only comes from the unregistered BazelGoImportResolver
class GoPackageDocumentationProvider extends DocumentationProviderEx {
    public GoPackageDocumentationProvider() {
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element instanceof GoPackageFileSystemItem) {
            return CtrlMouseHandler.getInfo(element.getNavigationElement(), originalElement);
        }
        else {
            return null;
        }
    }
}
