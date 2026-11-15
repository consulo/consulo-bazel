package org.jetbrains.bazel.languages.bazelrc.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcReferenceSupport;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

// consulo: upstream PsiSymbolReference own references are classic references
@ExtensionImpl
class BazelrcReferenceSupportImpl implements BazelrcReferenceSupport {
    public BazelrcReferenceSupportImpl() {
    }

    @Override
    @RequiredReadAction
    public Collection<PsiReference> getOwnReferences(BazelrcFlag element) {
        return element.getNameElement() != null ? List.of(new BazelrcFlagNameReference(element)) : List.of();
    }

    @Override
    public @Nullable PsiReference getReference(BazelrcImport element) {
        PsiReference[] references = element.getReferences();
        // consulo: Kotlin Array.last() throws on an empty array
        if (references.length == 0) {
            throw new NoSuchElementException("Array is empty.");
        }
        return references[references.length - 1];
    }

    @Override
    @RequiredReadAction
    public PsiReference[] getReferences(BazelrcImport element) {
        return new BazelrcImportFileReferenceSet(element).getAllReferences();
    }
}
