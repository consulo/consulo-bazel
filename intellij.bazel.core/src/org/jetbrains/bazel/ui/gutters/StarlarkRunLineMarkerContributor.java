package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.runnerAction.BuildTargetAction;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// consulo: upstream isDumbAware() = true -> DumbAware
@ExtensionImpl
public class StarlarkRunLineMarkerContributor extends RunLineMarkerContributor implements DumbAware {
    protected boolean isProjectApplicable(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    @RequiredReadAction
    @Override
    public @Nullable Info getInfo(PsiElement element) {
        if (!isProjectApplicable(element.getProject())) {
            return null;
        }
        PsiElement parent = element.getParent();
        PsiElement grandParent = parent != null ? parent.getParent() : null;
        if (grandParent == null) {
            return null;
        }
        if (shouldAddMarker(element, grandParent)) {
            return calculateMarkerInfo(grandParent);
        }
        else {
            return null;
        }
    }

    private boolean shouldAddMarker(PsiElement receiver, PsiElement grandParent) {
        return PsiUtilCore.getElementType(receiver) == StarlarkTokenTypes.IDENTIFIER &&
            grandParent instanceof StarlarkCallExpression &&
            isTopLevelCall(
                grandParent
            );
    }

    private boolean isTopLevelCall(PsiElement element) {
        PsiElement parent = element.getParent();
        return parent instanceof StarlarkExpressionStatement && parent.getParent() instanceof StarlarkFile;
    }

    @RequiredReadAction
    private @Nullable Info calculateMarkerInfo(PsiElement receiver) {
        VirtualFile virtualFile = Objects.requireNonNull(receiver.getContainingFile()).getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        if (BazelFileType.ofFileName(virtualFile.getName()) != BazelFileType.BUILD) {
            return null;
        }
        StarlarkCallExpressionVisitor visitor = new StarlarkCallExpressionVisitor();
        receiver.accept(visitor);
        String ruleName = visitor.ruleName;
        if (ruleName == null) {
            return null;
        }
        String targetName = visitor.targetName;
        if (targetName == null) {
            return null;
        }
        List<String> tags = visitor.tags;
        Project project = receiver.getProject();
        ResolvedLabel targetLabel = BazelRepoMappingUtils.calculateLabel(project, virtualFile, targetName);
        if (targetLabel == null) {
            return null;
        }
        Info info = calculateLineMarkerInfo(project, targetLabel, ruleName, tags);
        return info.actions.length > 0 ? info : null;
    }

    @RequiredReadAction
    private Info calculateLineMarkerInfo(
        Project project,
        ResolvedLabel targetLabel,
        String ruleName,
        @Nullable List<String> tags
    ) {
        AnAction[] actions = calculateEligibleActions(project, targetLabel, ruleName, tags).toArray(AnAction.EMPTY_ARRAY);
        boolean hasRunAction = false;
        for (AnAction it : actions) {
            if (!(it instanceof BuildTargetAction)) {
                hasRunAction = true;
                break;
            }
        }
        return new Info(
            hasRunAction ? PlatformIconGroup.actionsExecute() : PlatformIconGroup.actionsCompile(),
            null,
            actions
        );
    }

    @RequiredReadAction
    private List<AnAction> calculateEligibleActions(
        Project project,
        ResolvedLabel targetLabel,
        String ruleName,
        @Nullable List<String> tags
    ) {
        List<AnAction> result = new ArrayList<>();
        TargetStorage targetUtils = TargetStorage.getTargetStorage(project);
        BuildTarget targetInfo = targetUtils.getTargetSummary(targetLabel);
        TargetKind targetKind = targetInfo != null ? targetInfo.getKind() : TargetKindService.getInstance().guessFromRuleName(ruleName);
        // consulo: upstream shadows the tags parameter with this local value
        List<String> targetTags = targetInfo != null ? targetInfo.getTags() : tags != null ? tags : List.of();

        result.add(new BuildTargetAction(targetLabel));

        if (targetKind.isExecutable()) {
            BuildTarget executableTarget =
                targetInfo != null ? targetInfo : new NonImportedBuildTarget(targetLabel, targetKind, targetTags);
            result.addAll(ExecutorActionsUtil.getExecutorActions(project, executableTarget));
        }
        return result;
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}

class StarlarkCallExpressionVisitor extends StarlarkElementVisitor {
    @Nullable String ruleName = null;
    @Nullable String targetName = null;
    @Nullable List<String> tags = null;

    @Override
    public void visitCallExpression(StarlarkCallExpression node) {
        ruleName = node.getCalledFunctionName();
        targetName = node.getNameAttributeValue();
        tags = node.getTags();
    }
}
