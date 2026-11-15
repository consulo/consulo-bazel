package org.jetbrains.bazel.languages.bazelrc.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage.BazelrcFileType;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.jetbrains.bazel.config.project.getRootDir;

class BazelrcImportReference extends FileReference {
    BazelrcImportReference(
        FileReferenceSet referenceSet,
        TextRange range,
        int index,
        String text
    ) {
        super(referenceSet, range, index, text);
    }

    @Override
    protected ResolveResult[] innerResolve(boolean caseSensitive, PsiFile containingFile) {
        List<PsiElement> results = new ArrayList<>();
        Project project = getElement().getProject();
        if (getText().equals("%workspace%") && getIndex() == 0) {
            PsiFileSystemItem item = PsiUtilCore.findFileSystemItem(project, getRootDir(project));
            if (item != null) {
                results.add(item);
            }
        }
        else {
            List<VirtualFile> virtualFiles = new ArrayList<>();
            for (PsiFileSystemItem it : getContexts()) {
                VirtualFile virtualFile = it.getVirtualFile();
                VirtualFile child = virtualFile != null ? virtualFile.findChild(getText()) : null;
                if (child != null) {
                    virtualFiles.add(child);
                }
            }
            for (VirtualFile it : virtualFiles) {
                PsiFileSystemItem item = PsiUtilCore.findFileSystemItem(project, it);
                if (item != null) {
                    results.add(item);
                }
            }
        }
        return PsiElementResolveResult.createResults(results);
    }
}

class BazelrcImportFileReferenceSet extends FileReferenceSet {
    @RequiredReadAction
    BazelrcImportFileReferenceSet(BazelrcImport importSpec) {
        super(
            importPathText(importSpec),
            importSpec,
            importPathStartOffsetInParent(importSpec),
            null,
            true,
            true,
            new FileType[]{BazelrcFileType.INSTANCE}
        );
    }

    @RequiredReadAction
    private static String importPathText(BazelrcImport importSpec) {
        PsiElement importPath = importSpec.getImportPath();
        return importPath != null ? importPath.getText() : "";
    }

    @RequiredReadAction
    private static int importPathStartOffsetInParent(BazelrcImport importSpec) {
        PsiElement importPath = importSpec.getImportPath();
        return importPath != null ? importPath.getStartOffsetInParent() : 0;
    }

    @Override
    public FileReference createFileReference(
        TextRange range,
        int index,
        String text
    ) {
        return new BazelrcImportReference(this, range, index, text);
    }

    // consulo: Kotlin listOf(nullable) keeps a null parent
    @Override
    public Collection<PsiFileSystemItem> computeDefaultContexts() {
        return Collections.singletonList(Objects.requireNonNull(getElement().getContainingFile()).getOriginalFile().getParent());
    }

    @Override
    public Predicate<PsiFileSystemItem> getReferenceCompletionFilter() {
        return it -> (it.isDirectory() && !it.getName().startsWith(".")) || it.getName().endsWith(".bazelrc");
    }
}
