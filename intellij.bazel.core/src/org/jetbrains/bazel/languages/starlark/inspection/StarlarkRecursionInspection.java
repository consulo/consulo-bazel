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
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable;
import org.jetbrains.bazel.languages.starlark.utils.GraphUtils;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkCallableResolver;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public class StarlarkRecursionInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new RecursionVisitor(holder);
    }

    private static class RecursionVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;
        private final Set<PsiElement> myReported = new HashSet<>();

        RecursionVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {
            if (!(psiFile instanceof StarlarkFile file)) {
                return;
            }
            StarlarkCallGraph callGraph = StarlarkCallGraph.build(file);

            callGraph.recursiveCalls().forEach(call -> {
                PsiElement firstChild = call.getFirstChild();
                PsiElement element = firstChild != null ? firstChild : call;
                if (myReported.add(element)) {
                    myHolder.newProblem(StarlarkLocalize.inspectionDescriptionRecursionDetected())
                        .range(element)
                        .create();
                }
            });
        }
    }

    private static class StarlarkCallGraph {
        private final Map<StarlarkCallable, List<ResolvedCall>> myResolvedCallsByCallable;
        private final Map<StarlarkCallable, Integer> myCyclicComponentByCallable;

        private StarlarkCallGraph(
            Map<StarlarkCallable, List<ResolvedCall>> resolvedCallsByCallable,
            Map<StarlarkCallable, Integer> cyclicComponentByCallable
        ) {
            myResolvedCallsByCallable = resolvedCallsByCallable;
            myCyclicComponentByCallable = cyclicComponentByCallable;
        }

        record ResolvedCall(StarlarkCallExpression call, StarlarkCallable target) {
        }

        public List<StarlarkCallExpression> recursiveCalls() {
            List<StarlarkCallExpression> result = new ArrayList<>();
            for (Map.Entry<StarlarkCallable, List<ResolvedCall>> entry : myResolvedCallsByCallable.entrySet()) {
                Integer sourceComponent = myCyclicComponentByCallable.get(entry.getKey());
                for (ResolvedCall it : entry.getValue()) {
                    if (sourceComponent != null && Objects.equals(myCyclicComponentByCallable.get(it.target()), sourceComponent)) {
                        result.add(it.call());
                    }
                }
            }
            return result;
        }

        public static StarlarkCallGraph build(StarlarkFile file) {
            Map<StarlarkCallable, List<StarlarkCallExpression>> callsByCallable = collectCallsByCallable(file);
            Collection<StarlarkCallable> callables = callsByCallable.keySet();
            Map<StarlarkCallable, List<ResolvedCall>> resolvedCallsByCallable = new LinkedHashMap<>();
            for (Map.Entry<StarlarkCallable, List<StarlarkCallExpression>> entry : callsByCallable.entrySet()) {
                List<ResolvedCall> resolvedCalls = new ArrayList<>();
                for (StarlarkCallExpression it : entry.getValue()) {
                    StarlarkCallable target = StarlarkCallableResolver.resolve(it);
                    if (target == null) {
                        continue;
                    }
                    if (!Objects.equals(target.getContainingFile(), file)) {
                        continue;
                    }
                    resolvedCalls.add(new ResolvedCall(it, target));
                }
                resolvedCallsByCallable.put(entry.getKey(), resolvedCalls);
            }
            Map<StarlarkCallable, Set<StarlarkCallable>> edges = new LinkedHashMap<>();
            for (Map.Entry<StarlarkCallable, List<ResolvedCall>> entry : resolvedCallsByCallable.entrySet()) {
                Set<StarlarkCallable> targets = new LinkedHashSet<>();
                for (ResolvedCall it : entry.getValue()) {
                    targets.add(it.target());
                }
                edges.put(entry.getKey(), targets);
            }

            return new StarlarkCallGraph(
                resolvedCallsByCallable,
                findCyclicComponentByCallable(callables, edges)
            );
        }

        private static Map<StarlarkCallable, List<StarlarkCallExpression>> collectCallsByCallable(StarlarkFile file) {
            Map<StarlarkCallable, List<StarlarkCallExpression>> callsByCallable = new LinkedHashMap<>();

            PsiTreeUtil.processElements(file, element -> {
                if (element instanceof StarlarkCallable callable) {
                    callsByCallable.put(callable, new ArrayList<>());
                }
                else if (element instanceof StarlarkCallExpression callExpression) {
                    StarlarkCallable owner = PsiTreeUtil.getParentOfType(callExpression, StarlarkCallable.class, true);
                    if (owner != null) {
                        callsByCallable.computeIfAbsent(owner, it -> new ArrayList<>()).add(callExpression);
                    }
                }
                return true;
            });

            return callsByCallable;
        }

        // In the call graph, recursion is represented by a cyclic strongly connected component
        // (several callables that can reach each other or a single callable with a self-call).
        private static Map<StarlarkCallable, Integer> findCyclicComponentByCallable(
            Collection<StarlarkCallable> callables,
            Map<StarlarkCallable, Set<StarlarkCallable>> edges
        ) {
            List<List<StarlarkCallable>> components = GraphUtils.findCyclicStronglyConnectedComponents(
                callables,
                it -> edges.getOrDefault(it, Set.of())
            );
            Map<StarlarkCallable, Integer> result = new LinkedHashMap<>();
            for (int index = 0; index < components.size(); index++) {
                for (StarlarkCallable it : components.get(index)) {
                    result.put(it, index);
                }
            }
            return result;
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkRecursion";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkRecursionCheck();
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
