/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.documentation;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.documentation.DocumentationProviderEx;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Serves upstream {@link DocumentationTargetProvider}s and {@link LookupElementDocumentationTargetProvider}s through the
 * classic Consulo documentation provider. Register one subclass per Bazel language.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class BazelDocumentationProviderBase extends DocumentationProviderEx implements LanguageDocumentationProvider {
    @Override
    public @Nullable PsiElement getCustomDocumentationElement(Editor editor, PsiFile file, @Nullable PsiElement contextElement) {
        if (contextElement == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        for (DocumentationTargetProvider provider : Application.get().getExtensionList(DocumentationTargetProvider.class)) {
            List<? extends DocumentationTarget> targets = provider.documentationTargets(file, offset);
            if (!targets.isEmpty()) {
                return new BazelDocumentationElement(contextElement, targets.get(0));
            }
        }
        return null;
    }

    @Override
    public @Nullable PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        if (!(object instanceof LookupElement lookupElement)) {
            return null;
        }
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }
        int offset = element.getTextRange().getStartOffset();
        for (LookupElementDocumentationTargetProvider provider : Application.get().getExtensionList(LookupElementDocumentationTargetProvider.class)) {
            DocumentationTarget target = provider.documentationTarget(file, lookupElement, offset);
            if (target != null) {
                return new BazelDocumentationElement(element, target);
            }
        }
        return null;
    }

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof BazelDocumentationElement documentationElement) {
            return DocumentationResult.toHtml(documentationElement.getTarget().computeDocumentation());
        }
        return null;
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element instanceof BazelDocumentationElement documentationElement) {
            return documentationElement.getTarget().computeDocumentationHint();
        }
        return null;
    }
}
