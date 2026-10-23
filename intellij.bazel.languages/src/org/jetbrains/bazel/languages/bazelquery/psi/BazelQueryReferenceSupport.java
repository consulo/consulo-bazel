package org.jetbrains.bazel.languages.bazelquery.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Builds references for bazelquery PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelQueryReferenceSupport {
    static @Nullable BazelQueryReferenceSupport getInstance() {
        return Application.get().getExtensionPoint(BazelQueryReferenceSupport.class).findFirstSafe(it -> true);
    }

    Collection<PsiReference> getOwnReferences(BazelQueryCommand element);

    Collection<PsiReference> getOwnReferences(BazelQueryFlag element);
}
