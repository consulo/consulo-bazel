package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkQualifiableExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkMutationUtils;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.bazel.languages.starlark.StarlarkUtils.relevantChildrenAfterEach;

@ExtensionImpl
public class StarlarkCollectionMutationDuringIterationInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new CollectionMutationVisitor(holder);
    }

    private static class CollectionMutationVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        CollectionMutationVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitForStatement(StarlarkForStatement node) {
            PsiElement collection = node.rightHandSide();
            if (collection == null) {
                return;
            }
            List<StarlarkStatementList> statementLists = node.getStatementLists();
            if (statementLists.isEmpty()) {
                return;
            }
            StarlarkStatementList body = statementLists.get(0);
            reportMutationsOfIteratedCollections(List.of(collection), body);
        }

        @Override
        public void visitListCompExpression(StarlarkListCompExpression node) {
            checkComprehension(node);
        }

        @Override
        public void visitDictCompExpression(StarlarkDictCompExpression node) {
            checkComprehension(node);
        }

        private void checkComprehension(StarlarkCompExpression node) {
            List<PsiElement> collections = relevantChildrenAfterEach(node, StarlarkTokenTypes.IN_KEYWORD);
            reportMutationsOfIteratedCollections(collections, node);
        }

        private void reportMutationsOfIteratedCollections(List<PsiElement> collectionExpressions, PsiElement scope) {
            List<PsiElement> iteratedCollections =
                collectionExpressions.stream().filter(StarlarkQualifiableExpression::isSimpleNameExpression).toList();
            if (iteratedCollections.isEmpty()) {
                return;
            }

            Set<PsiElement> reported = new HashSet<>();
            PsiTreeUtil.processElements(scope, it -> {
                if (it != scope && isInsideNestedCallable(it, scope)) {
                    return true;
                }

                StarlarkMutationUtils.Mutation mutation = StarlarkMutationUtils.mutationOrNull(it);
                if (mutation == null) {
                    return true;
                }
                boolean mutatesIteratedCollection = iteratedCollections.stream().anyMatch(collection ->
                    StarlarkMutationUtils.areReferencesEqual(mutation.target(), collection)
                );
                if (!mutatesIteratedCollection) {
                    return true;
                }

                if (reported.add(mutation.problemElement())) {
                    myHolder.newProblem(StarlarkLocalize.inspectionDescriptionCollectionMutationDuringIteration())
                        .range(mutation.problemElement())
                        .create();
                }

                return true;
            });
        }

        private boolean isInsideNestedCallable(PsiElement element, PsiElement outer) {
            PsiElement current = element.getParent();
            while (current != null && current != outer) {
                if (current instanceof StarlarkCallable) {
                    return true;
                }
                current = current.getParent();
            }
            return false;
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkCollectionMutationDuringIteration";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkCollectionMutationDuringIterationCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.starlarkInspectionGroupName();
    }

    // consulo: upstream groupPath="Bazel"
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{BazelPluginLocalize.bazelInspectionGroupName(), getGroupDisplayName()};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
