package org.jetbrains.bazel.languages.bazelquery.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiReference;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlag;
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryReferenceSupport;

import java.util.Collection;
import java.util.List;

// consulo: upstream PsiSymbolReference own references are classic references
@ExtensionImpl
class BazelQueryReferenceSupportImpl implements BazelQueryReferenceSupport {
    public BazelQueryReferenceSupportImpl() {
    }

    @Override
    @RequiredReadAction
    public Collection<PsiReference> getOwnReferences(BazelQueryCommand element) {
        return element.getNameElement() != null ? List.of(new BazelQueryFunctionReference(element)) : List.of();
    }

    @Override
    @RequiredReadAction
    public Collection<PsiReference> getOwnReferences(BazelQueryFlag element) {
        return element.getNameElement() != null ? List.of(new BazelQueryFlagNameReference(element)) : List.of();
    }
}
