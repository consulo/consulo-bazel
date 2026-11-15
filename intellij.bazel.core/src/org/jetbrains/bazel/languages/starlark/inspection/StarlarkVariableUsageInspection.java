package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator;

@ExtensionImpl
public class StarlarkVariableUsageInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new VariableUsagesVisitor(holder);
    }

    private static class VariableUsagesVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;
        private final Set<PsiElement> myReported = new HashSet<>();
        private @Nullable StarlarkScopeIndex myScopeIndex = null;
        private Set<String> myKnownGlobalNames = Set.of();

        VariableUsagesVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {
            if (!(psiFile instanceof StarlarkFile file)) {
                return;
            }
            myScopeIndex = StarlarkScopeIndex.getOrBuild(file);
            myKnownGlobalNames = BazelGlobalFunctions.globalFunctions(file.getProject()).keySet();
            collectReferences(file).forEach(this::checkReference);
        }

        private void checkReference(StarlarkReferenceExpression ref) {
            StarlarkScopeIndex index = myScopeIndex;
            if (index == null) {
                return;
            }
            String name = ref.getName();
            if (name == null) {
                return;
            }
            if (!shouldAnalyzeReference(ref)) {
                return;
            }

            StarlarkScopeIndex.RefUsage usage = index.analyzeReferenceUsage(ref);
            if (usage instanceof StarlarkScopeIndex.RefUsage.BeforeAssignment beforeAssignment) {
                registerOnce(ref, StarlarkLocalize.inspectionDescriptionVariableReferencedBeforeAssignment(beforeAssignment.kind(), name));
            }
            else if (usage instanceof StarlarkScopeIndex.RefUsage.Bound) {
                return;
            }
            else if (usage instanceof StarlarkScopeIndex.RefUsage.Unbound) {
                if (!isInsideComprehension(ref)) {
                    PsiReference reference = ref.getReference();
                    if (reference != null && reference.resolve() != null) {
                        return;
                    }
                }
                registerOnce(ref, StarlarkLocalize.inspectionDescriptionVariableUndefined(name));
            }
        }

        private boolean isInsideComprehension(StarlarkReferenceExpression ref) {
            return PsiTreeUtil.getParentOfType(ref, StarlarkCompExpression.class, false) != null;
        }

        private Collection<StarlarkReferenceExpression> collectReferences(StarlarkFile file) {
            return PsiTreeUtil.collectElementsOfType(file, StarlarkReferenceExpression.class);
        }

        private boolean shouldAnalyzeReference(StarlarkReferenceExpression ref) {
            String name = ref.getName();
            if (name == null) {
                return false;
            }
            if (name.equals("_")) {
                return false;
            }
            if (isAssignmentBinding(ref)) {
                return false;
            }
            if (isQualifiedReference(ref)) {
                return false;
            }
            if (myKnownGlobalNames.contains(name)) {
                return false;
            }
            return true;
        }

        private boolean isAssignmentBinding(StarlarkReferenceExpression ref) {
            StarlarkAssignmentStatement assignment = PsiTreeUtil.getParentOfType(ref, StarlarkAssignmentStatement.class);
            if (assignment == null) {
                return false;
            }
            PsiElement lhs = assignment.leftHandSide();
            if (lhs == null) {
                return false;
            }

            return containsReference(lhs, ref);
        }

        // consulo: upstream local fun containsReference(target) inside isAssignmentBinding
        private boolean containsReference(PsiElement target, StarlarkReferenceExpression ref) {
            if (target.equals(ref)) {
                return true;
            }
            else if (target instanceof StarlarkSubscriptionExpression) {
                return false;
            }
            else if (target instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
                StarlarkTupleExpression tuple = parenthesizedExpression.getTuple();
                return tuple != null && containsReference(tuple, ref);
            }
            else if (target instanceof StarlarkTupleExpression tupleExpression) {
                return tupleExpression.getTargetExpressions().stream().anyMatch(it -> containsReference(it, ref));
            }
            else if (target instanceof StarlarkListLiteralExpression) {
                PsiElement child = nearestRelevantBeforeOperator(target.getFirstChild());
                while (child != null) {
                    if (containsReference(child, ref)) {
                        return true;
                    }
                    child = nearestRelevantBeforeOperator(child.getNextSibling());
                }
                return false;
            }
            else {
                return false;
            }
        }

        private boolean isQualifiedReference(StarlarkReferenceExpression ref) {
            if (ref.getQualifierExpression() != null) {
                return true;
            }
            StarlarkReferenceExpression parentRef = ref.getParent() instanceof StarlarkReferenceExpression referenceExpression
                ? referenceExpression
                : null;
            return parentRef != null && ref.equals(parentRef.getQualifierExpression());
        }

        private void registerOnce(PsiElement element, LocalizeValue message) {
            if (myReported.add(element)) {
                myHolder.newProblem(message).range(element).create();
            }
        }
    }


    private static class StarlarkScopeIndex {
        private final Map<PsiElement, ScopeData> myScopeDataByScope;

        private StarlarkScopeIndex(Map<PsiElement, ScopeData> scopeDataByScope) {
            myScopeDataByScope = scopeDataByScope;
        }

        enum ScopeKind {
            FILE, FUNCTION
        }

        record ScopeData(
            Map<String, Integer> firstAssignmentByName,
            Map<String, List<Integer>> callOffsetsByFunctionName,
            @Nullable PsiElement parentScope,
            ScopeKind kind
        ) {
            public boolean isFunctionScope() {
                return kind == ScopeKind.FUNCTION;
            }

            public String getLabel() {
                return switch (kind) {
                    case FILE -> GLOBAL;
                    case FUNCTION -> LOCAL;
                };
            }
        }

        sealed interface RefUsage {
            record BeforeAssignment(String kind) implements RefUsage {
            }

            // consulo: Kotlin data object
            record Bound() implements RefUsage {
                static final Bound INSTANCE = new Bound();
            }

            // consulo: Kotlin data object
            record Unbound() implements RefUsage {
                static final Unbound INSTANCE = new Unbound();
            }
        }

        public RefUsage analyzeReferenceUsage(StarlarkReferenceExpression ref) {
            String name = ref.getName();
            if (name == null) {
                return RefUsage.Unbound.INSTANCE;
            }
            PsiElement currentScope = nearestScopeOwner(ref);
            if (currentScope == null) {
                return RefUsage.Unbound.INSTANCE;
            }
            ScopeData currentScopeData = scopeData(currentScope);
            if (currentScopeData == null) {
                return RefUsage.Unbound.INSTANCE;
            }

            if (isBoundByEnclosingComprehension(ref, name)) {
                return RefUsage.Bound.INSTANCE;
            }

            Integer offset = currentScopeData.firstAssignmentByName().get(name);
            if (offset != null) {
                return ref.getTextOffset() < offset ? new RefUsage.BeforeAssignment(currentScopeData.getLabel()) : RefUsage.Bound.INSTANCE;
            }

            PsiElement outer = currentScopeData.parentScope();
            while (outer != null) {
                ScopeData outerData = scopeData(outer);
                if (outerData == null) {
                    break;
                }
                Integer outerFirstOffset = outerData.firstAssignmentByName().get(name);
                if (outerFirstOffset != null) {
                    if (outerData.isFunctionScope() && isFreeRefBeforeOuterAssignment(currentScope, outerData, outerFirstOffset)) {
                        return new RefUsage.BeforeAssignment(FREE);
                    }
                    return RefUsage.Bound.INSTANCE;
                }
                outer = outerData.parentScope();
            }

            return RefUsage.Unbound.INSTANCE;
        }

        private boolean isBoundByEnclosingComprehension(StarlarkReferenceExpression ref, String name) {
            StarlarkCompExpression comp = PsiTreeUtil.getParentOfType(ref, StarlarkCompExpression.class, false);
            while (comp != null) {
                List<StarlarkTargetExpression> targets = comp.getCompVariables();
                StarlarkTargetExpression firstTarget = null;
                for (StarlarkTargetExpression target : targets) {
                    if (firstTarget == null || target.getTextOffset() < firstTarget.getTextOffset()) {
                        firstTarget = target;
                    }
                }
                if (firstTarget != null && !isInFirstComprehensionIterable(ref, firstTarget)) {
                    boolean isBodyExpression = ref.getTextOffset() < firstTarget.getTextOffset();
                    if (targets.stream().anyMatch(it -> name.equals(it.getName()) && (isBodyExpression || it.getTextOffset() < ref.getTextOffset()))) {
                        return true;
                    }
                }
                comp = PsiTreeUtil.getParentOfType(comp, StarlarkCompExpression.class, true);
            }
            return false;
        }

        private boolean isInFirstComprehensionIterable(StarlarkReferenceExpression ref, StarlarkTargetExpression firstTarget) {
            PsiElement iterable = findComprehensionIterableForTarget(firstTarget);
            return iterable != null && PsiTreeUtil.isAncestor(iterable, ref, false);
        }

        private @Nullable PsiElement findComprehensionIterableForTarget(StarlarkTargetExpression target) {
            PsiElement current = target;
            while (current != null && !(current instanceof StarlarkCompExpression)) {
                PsiElement next = PsiTreeUtil.skipWhitespacesAndCommentsForward(current);
                if (next != null && PsiUtilCore.getElementType(next.getNode()) == StarlarkTokenTypes.IN_KEYWORD) {
                    return PsiTreeUtil.skipWhitespacesAndCommentsForward(next);
                }
                current = current.getParent();
            }
            return null;
        }

        // Handles simple outer->inner calls only. Does not cover deep call chains or indirect calls (e.g., via aliases or containers).
        private boolean isFreeRefBeforeOuterAssignment(PsiElement currentScope, ScopeData outerData, int outerFirstOffset) {
            if (!(currentScope instanceof StarlarkFunctionDeclaration innerFunction)) {
                return false;
            }
            String innerFunctionName = innerFunction.getName();
            if (innerFunctionName == null) {
                return false;
            }
            List<Integer> callOffsets = outerData.callOffsetsByFunctionName().get(innerFunctionName);
            if (callOffsets == null) {
                return false;
            }
            return callOffsets.stream().anyMatch(it -> it < outerFirstOffset);
        }

        private @Nullable PsiElement nearestScopeOwner(PsiElement element) {
            PsiElement current = element;
            while (current != null) {
                if (isScopeOwner(current) && !isInOwnParameterList(element, current)) {
                    return current;
                }
                current = current.getParent();
            }
            return null;
        }

        private boolean isInOwnParameterList(PsiElement element, PsiElement scopeOwner) {
            if (!(scopeOwner instanceof StarlarkCallable)) {
                return false;
            }
            StarlarkParameterList parameterList = PsiTreeUtil.getChildOfType(scopeOwner, StarlarkParameterList.class);
            if (parameterList == null) {
                return false;
            }
            return PsiTreeUtil.isAncestor(parameterList, element, false);
        }

        private @Nullable ScopeData scopeData(PsiElement scope) {
            return myScopeDataByScope.get(scope);
        }

        private static final String GLOBAL = "Global";
        private static final String LOCAL = "Local";
        private static final String FREE = "Free";

        public static StarlarkScopeIndex getOrBuild(StarlarkFile file) {
            return LanguageCachedValueUtil.getCachedValue(file, () ->
                CachedValueProvider.Result.create(build(file), PsiModificationTracker.MODIFICATION_COUNT, file)
            );
        }

        private static StarlarkScopeIndex build(StarlarkFile file) {
            record MutableScopeData(
                LinkedHashMap<String, Integer> firstAssignmentByName,
                LinkedHashMap<String, List<Integer>> callOffsetsByFunctionName,
                @Nullable PsiElement parentScope,
                ScopeKind kind
            ) {
            }

            Map<PsiElement, MutableScopeData> mutable = new LinkedHashMap<>();

            // consulo: upstream local funs of build(); a local class keeps them together
            class Builder {
                void ensureScope(PsiElement scope, @Nullable PsiElement parentScope) {
                    if (!mutable.containsKey(scope)) {
                        ScopeKind kind;
                        if (scope instanceof StarlarkFile) {
                            kind = ScopeKind.FILE;
                        }
                        else if (scope instanceof StarlarkFunctionDeclaration || scope instanceof StarlarkLambdaExpression) {
                            kind = ScopeKind.FUNCTION;
                        }
                        else {
                            throw new IllegalStateException("Unexpected scope type: " + scope.getClass().getName());
                        }
                        mutable.put(scope, new MutableScopeData(
                            new LinkedHashMap<>(),
                            new LinkedHashMap<>(),
                            parentScope,
                            kind
                        ));
                    }
                }

                void putFirstBinding(@Nullable PsiElement scope, @Nullable String name, int offset) {
                    if (name == null) {
                        return;
                    }
                    MutableScopeData data = scope == null ? null : mutable.get(scope);
                    if (data == null) {
                        return;
                    }
                    if (!data.firstAssignmentByName().containsKey(name)) {
                        data.firstAssignmentByName().put(name, offset);
                    }
                }

                void putCallableParameters(PsiElement scope, StarlarkCallable callable) {
                    for (StarlarkParameter it : callable.getParameters()) {
                        putFirstBinding(scope, it.getName(), it.getTextOffset());
                    }
                }

                void putTargetBindings(@Nullable PsiElement scope, @Nullable PsiElement target) {
                    if (target instanceof StarlarkTargetExpression targetExpression) {
                        putFirstBinding(scope, targetExpression.getName(), targetExpression.getTextOffset());
                    }
                    else if (target instanceof StarlarkReferenceExpression referenceExpression) {
                        putFirstBinding(scope, referenceExpression.getName(), referenceExpression.getTextOffset());
                    }
                    else if (target instanceof StarlarkParenthesizedExpression parenthesizedExpression) {
                        putTargetBindings(scope, parenthesizedExpression.getTuple());
                    }
                    else if (target instanceof StarlarkTupleExpression tupleExpression) {
                        for (StarlarkTargetExpression it : tupleExpression.getTargetExpressions()) {
                            putFirstBinding(scope, it.getName(), it.getTextOffset());
                        }
                    }
                    else if (target instanceof StarlarkListLiteralExpression) {
                        PsiElement child = nearestRelevantBeforeOperator(target.getFirstChild());
                        while (child != null) {
                            putTargetBindings(scope, child);
                            child = nearestRelevantBeforeOperator(child.getNextSibling());
                        }
                    }
                }

                @Nullable String loadSymbolLocalName(StarlarkLoadValue loadValue) {
                    if (loadValue instanceof StarlarkNamedLoadValue namedLoadValue) {
                        return namedLoadValue.getName();
                    }
                    else if (loadValue instanceof StarlarkStringLoadValue stringLoadValue) {
                        return stringLoadValue.getLoadValueExpressionContent();
                    }
                    else {
                        return null;
                    }
                }

                void walk(PsiElement node, @Nullable PsiElement currentScope) {
                    PsiElement scopeForNode = isScopeOwner(node) ? node : currentScope;
                    if (scopeForNode == node) {
                        // consulo: scopeForNode is node here (passed directly for null analysis)
                        ensureScope(node, currentScope);
                    }

                    if (node instanceof StarlarkAssignmentStatement assignmentStatement) {
                        putTargetBindings(currentScope, assignmentStatement.leftHandSide());
                    }
                    else if (node instanceof StarlarkFunctionDeclaration functionDeclaration) {
                        putFirstBinding(currentScope, functionDeclaration.getName(), functionDeclaration.getTextOffset());
                        putCallableParameters(functionDeclaration, functionDeclaration);
                    }
                    else if (node instanceof StarlarkLambdaExpression lambdaExpression) {
                        putCallableParameters(lambdaExpression, lambdaExpression);
                    }
                    else if (node instanceof StarlarkForStatement forStatement) {
                        for (StarlarkTargetExpression it : forStatement.getLoopVariables()) {
                            putFirstBinding(scopeForNode, it.getName(), it.getTextOffset());
                        }
                    }
                    else if (node instanceof StarlarkLoadStatement loadStatement) {
                        for (StarlarkElement symbol : loadStatement.getLoadedSymbolsPsi()) {
                            if (!(symbol instanceof StarlarkLoadValue it)) {
                                continue;
                            }
                            StarlarkStringLiteralExpression loadValueExpression = it.getLoadValueExpression();
                            putFirstBinding(
                                scopeForNode,
                                loadSymbolLocalName(it),
                                loadValueExpression != null ? loadValueExpression.getTextOffset() : it.getTextOffset()
                            );
                        }
                    }
                    else if (node instanceof StarlarkCallExpression callExpression) {
                        String calledName = callExpression.getCalledFunctionName();
                        MutableScopeData data = scopeForNode == null ? null : mutable.get(scopeForNode);
                        if (calledName != null && data != null) {
                            data.callOffsetsByFunctionName().computeIfAbsent(calledName, k -> new ArrayList<>()).add(callExpression.getTextOffset());
                        }
                    }

                    PsiElement child = node.getFirstChild();
                    while (child != null) {
                        walk(child, scopeForNode);
                        child = child.getNextSibling();
                    }
                }
            }

            new Builder().walk(file, null);

            Map<PsiElement, ScopeData> immutable = new LinkedHashMap<>();
            for (Map.Entry<PsiElement, MutableScopeData> entry : mutable.entrySet()) {
                MutableScopeData data = entry.getValue();
                Map<String, List<Integer>> callOffsetsByFunctionName = new LinkedHashMap<>();
                for (Map.Entry<String, List<Integer>> callEntry : data.callOffsetsByFunctionName().entrySet()) {
                    callOffsetsByFunctionName.put(callEntry.getKey(), List.copyOf(callEntry.getValue()));
                }
                immutable.put(entry.getKey(), new ScopeData(
                    new LinkedHashMap<>(data.firstAssignmentByName()),
                    callOffsetsByFunctionName,
                    data.parentScope(),
                    data.kind()
                ));
            }

            return new StarlarkScopeIndex(immutable);
        }

        private static boolean isScopeOwner(PsiElement element) {
            return element instanceof StarlarkFile ||
                element instanceof StarlarkFunctionDeclaration ||
                element instanceof StarlarkLambdaExpression;
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkVariableUsage";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkVariableUsageCheck();
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
