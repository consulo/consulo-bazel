package org.jetbrains.bazel.languages.starlark.utils;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Heuristically evaluates "srcs = ..." and "resources == ..." expressions and find targets which may
 * include the given source file as a source
 */
public class StarlarkSrcsListEval {
    private final Project myProject;

    public StarlarkSrcsListEval(Project project) {
        myProject = project;
    }

    private final Map<VirtualFile, List<StarlarkTargetInfo>> myFileTargetsCache = new HashMap<>();

    public @Nullable VirtualFile findBuildFileForSourceFile(VirtualFile file) {
        VirtualFile root = BazelRepoMappingUtils.findContainingBazelRepo(file);
        if (root == null) {
            return null;
        }
        return LabelResolveUtils.findBuildFilePathFor(file, root);
    }

    @RequiredReadAction
    public Map<Label, EnumSet<Kind>> findTargetsForSourceFile(VirtualFile file) {
        VirtualFile buildFile = findBuildFileForSourceFile(file);
        if (buildFile == null) {
            return Map.of();
        }

        String relativePath = VirtualFileUtil.getRelativePath(file, Objects.requireNonNull(buildFile.getParent()));
        if (relativePath == null) {
            return Map.of();
        }

        List<StarlarkTargetInfo> targets = myFileTargetsCache.computeIfAbsent(buildFile, this::calculateTargets);
        Map<Label, EnumSet<Kind>> result = new LinkedHashMap<>();
        for (StarlarkTargetInfo target : targets) {
            EnumSet<Kind> set = EnumSet.noneOf(Kind.class);
            if (target.mySrcsExpr != null && StarlarkGlobUtils.srcsExpressionMatchPath(target.mySrcsExpr, relativePath)) {
                set.add(Kind.Srcs);
            }
            if (target.myResourcesExpr != null && StarlarkGlobUtils.srcsExpressionMatchPath(target.myResourcesExpr, relativePath)) {
                set.add(Kind.Resources);
            }
            if (set.isEmpty()) {
                continue;
            }
            result.put(target.myLabel, set);
        }
        return result;
    }

    private List<StarlarkTargetInfo> calculateTargets(VirtualFile buildFile) {
        if (!(PsiManager.getInstance(myProject).findFile(buildFile) instanceof StarlarkFile starlarkFile)) {
            return List.of();
        }

        List<StarlarkTargetInfo> result = new ArrayList<>();
        for (StarlarkCallExpression targetRuleExpr : starlarkFile.getTargetRules()) {
            String ruleName = targetRuleExpr.getCalledFunctionName();
            String targetName = targetRuleExpr.getNameAttributeValue();
            ResolvedLabel targetLabel = BazelRepoMappingUtils.calculateLabel(myProject, buildFile, targetName);
            if (targetLabel == null) {
                continue;
            }

            StarlarkArgumentList argumentList = targetRuleExpr.getArgumentList();
            StarlarkNamedArgumentExpression srcsArgument = argumentList == null ? null : argumentList.getSrcsArgument();
            PsiElement srcsExpression = srcsArgument == null ? null : srcsArgument.getValue();
            StarlarkNamedArgumentExpression resourcesArgument = argumentList == null ? null : argumentList.getResourcesArgument();
            PsiElement resourcesExpression = resourcesArgument == null ? null : resourcesArgument.getValue();

            result.add(new StarlarkTargetInfo(ruleName, targetLabel, srcsExpression, resourcesExpression));
        }
        return result;
    }

    private static class StarlarkTargetInfo {
        private final @Nullable String myKind;
        private final Label myLabel;
        private final @Nullable PsiElement mySrcsExpr;
        private final @Nullable PsiElement myResourcesExpr;

        private StarlarkTargetInfo(
            @Nullable String kind,
            Label label,
            @Nullable PsiElement srcsExpr,
            @Nullable PsiElement resourcesExpr
        ) {
            myKind = kind;
            myLabel = label;
            mySrcsExpr = srcsExpr;
            myResourcesExpr = resourcesExpr;
        }
    }

    public enum Kind {
        Srcs, Resources
    }
}
