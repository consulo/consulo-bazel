package org.jetbrains.bazel.languages.starlark.annotation;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.util.query.Query;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.colorScheme.TextAttributes;
import consulo.content.scope.SearchScope;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.project.DumbService;
import consulo.ui.style.StandardColors;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadsIndex;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.Objects;

// consulo: Consulo registers annotators through AnnotatorFactory; the stateless annotator is its own factory
@ExtensionImpl
public class StarlarkDeclarationAnnotator implements Annotator, AnnotatorFactory, DumbAware {
    // consulo: JBColor.GRAY -> StandardColors.GRAY (Consulo TextAttributes takes ColorValue)
    private static final TextAttributes UNUSED_TEXT_ATTRIBUTES = new TextAttributes(StandardColors.GRAY, null, null, null, Font.PLAIN);

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (isUnusedDeclaration(element)) {
            String name = element.getParent() instanceof StarlarkNamedElement namedElement ? namedElement.getName() : null;
            annotateUnused(holder, element, name != null ? name : "");
        }
    }

    private boolean isUnusedDeclaration(PsiElement receiver) {
        if (!isDeclaration(receiver)) {
            return false;
        }
        PsiElement parent = Objects.requireNonNull(receiver.getParent());
        // Not a variable — unused if it does not refer to anything and is also unreferenced.
        if (!(parent instanceof StarlarkTargetExpression targetExpression)) {
            PsiReference reference = parent.getReference();
            return (reference == null || reference.resolve() == null) && isUnreferenced(parent);
        }
        // If a variable is not inside assignment, we consider it a fresh declaration
        if (!isInsideAssignStatement(targetExpression)) {
            return isUnreferenced(targetExpression);
        }
        PsiReference reference = targetExpression.getReference();
        PsiElement originalAssignment = reference == null ? null : reference.resolve();
        if (originalAssignment == null) {
            return isUnreferenced(targetExpression);
        }
        // A reassignment is unused only when the canonical (first) binding itself is
        // unreferenced — i.e. nothing besides other reassignments refers to it.
        return isUnreferenced(originalAssignment);
    }

    private boolean isDeclaration(PsiElement receiver) {
        PsiElement parent = receiver.getParent();
        return PsiUtilCore.getElementType(receiver) == StarlarkTokenTypes.IDENTIFIER &&
            parent instanceof StarlarkNamedElement &&
            !(parent instanceof StarlarkVariadicParameter) &&
            !(parent instanceof StarlarkKeywordVariadicParameter);
    }

    private boolean isUnreferenced(PsiElement receiver) {
        if (shouldSkipUnreferencedCheck(receiver)) {
            return false;
        }
        SearchScope scope = receiver.getUseScope();
        if (scope instanceof GlobalSearchScope globalSearchScope &&
            receiver instanceof StarlarkNamedElement namedElement &&
            StarlarkLoadsIndex.isLoaded(namedElement, globalSearchScope)) {
            return false;
        }
        // if not accessed externally, check for local usages
        SearchScope limitedScope = scope.intersectWith(new LocalSearchScope(Objects.requireNonNull(receiver.getContainingFile())));
        Query<PsiReference> references = ReferencesSearch.search(receiver, limitedScope);
        if (!(receiver instanceof StarlarkTargetExpression)) {
            return references.findFirst() == null;
        }
        // Variable is unreferenced if only reassignments refer to it.
        return !references.anyMatch(it -> !(it.getElement() instanceof StarlarkTargetExpression));
    }

    private boolean shouldSkipUnreferencedCheck(PsiElement receiver) {
        if (!isStarlarkTopLevel(receiver) && isExplicitlyUnused(receiver)) {
            return true;
        }
        // it should be possible to always check local declarations usage
        if (!(receiver.getUseScope() instanceof GlobalSearchScope)) {
            return false;
        }
        // TODO consulo: SyntheticPsiFileSupport.isOutsiderFile(containingFile.virtualFile) is missing in Consulo
        return DumbService.isDumb(receiver.getProject()) /* || SyntheticPsiFileSupport.isOutsiderFile(containingFile.virtualFile) */;
    }

    // In Starlark, _-prefixed names signal intentionally unused declarations (like Python's _ convention).
    private boolean isExplicitlyUnused(PsiElement receiver) {
        if (receiver instanceof StarlarkNamedElement namedElement) {
            String name = namedElement.getName();
            return name != null && name.startsWith("_");
        }
        else {
            return false;
        }
    }

    private boolean isStarlarkTopLevel(PsiElement receiver) {
        if (receiver instanceof StarlarkTargetExpression targetExpression) {
            return targetExpression.isTopLevel();
        }
        else if (receiver instanceof StarlarkFunctionDeclaration functionDeclaration) {
            return functionDeclaration.isTopLevel();
        }
        else {
            return false;
        }
    }

    private boolean isInsideAssignStatement(StarlarkTargetExpression receiver) {
        PsiElement current = receiver;
        while (true) {
            PsiElement parent = current.getParent();
            if (parent instanceof StarlarkAssignmentStatement) {
                return true;
            }
            else if (parent instanceof StarlarkTupleExpression || parent instanceof StarlarkParenthesizedExpression) {
                current = parent;
            }
            else {
                return false;
            }
        }
    }

    private void annotateUnused(AnnotationHolder receiver, PsiElement element, String name) {
        receiver.newAnnotation(
            HighlightSeverity.WEAK_WARNING,
            StarlarkLocalize.annotatorUnusedDeclaration(declarationTypeOfNamedElement(Objects.requireNonNull(element.getParent())), name)
        ).range(element).enforcedTextAttributes(UNUSED_TEXT_ATTRIBUTES).create();
    }

    private String declarationTypeOfNamedElement(PsiElement element) {
        if (element instanceof StarlarkNamedLoadValue) {
            return "Load value";
        }
        else if (element instanceof StarlarkFunctionDeclaration) {
            return "Function";
        }
        else if (element instanceof StarlarkTargetExpression) {
            return "Variable";
        }
        else if (element instanceof StarlarkParameter) {
            return "Parameter";
        }
        else {
            return "";
        }
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public @Nullable Annotator createAnnotator() {
        return this;
    }
}
