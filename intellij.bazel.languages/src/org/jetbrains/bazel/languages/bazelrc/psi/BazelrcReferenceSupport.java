package org.jetbrains.bazel.languages.bazelrc.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Builds references for bazelrc PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelrcReferenceSupport {
    static @Nullable BazelrcReferenceSupport getInstance() {
        return Application.get().getExtensionPoint(BazelrcReferenceSupport.class).findFirstSafe(it -> true);
    }

    Collection<PsiReference> getOwnReferences(BazelrcFlag element);

    @Nullable PsiReference getReference(BazelrcImport element);

    PsiReference[] getReferences(BazelrcImport element);
}
