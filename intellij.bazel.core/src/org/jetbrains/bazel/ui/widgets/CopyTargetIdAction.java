package org.jetbrains.bazel.ui.widgets;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.ReadAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1158

/**
 * Action is available in a BUILD file in the right click Editor Popup Menu under Copy/Paste Special
 * while pressing on rule's name or value of the name argument of that rule.
 * Also available in the right click menu on regular source files that are included in project.
 */
@ActionImpl(
    id = "CopyTargetIdAction",
    parents = {
        @ActionParentRef(@ActionRef(id = "EditorPopupMenu")),
        @ActionParentRef(@ActionRef(id = "EditorTabPopupMenu")),
    }
)
// This shortcut doesn't mean anything, it's just not taken by anyone and is easy to type
// TODO consulo: default shortcut control alt G, H
public class CopyTargetIdAction extends SuspendableAction {
    private static final List<String> ALLOWED_ACTION_PLACES =
        List.of(
            ActionPlaces.EDITOR_POPUP,
            ActionPlaces.KEYBOARD_SHORTCUT,
            ActionPlaces.ACTION_SEARCH,
            ActionPlaces.EDITOR_TAB_POPUP
        );

    public CopyTargetIdAction() {
        // consulo: the icon comes from the upstream XML registration
        super(BazelPluginLocalize.editorActionCopyTargetId(), BazelPluginIcons.bazel());
    }

    @Override
    protected CompletableFuture<Void> actionPerformed(Project project, AnActionEvent e) {
        // consulo: readAction { } -> blocking read action
        @Nullable PsiFile psiFile = ReadAction.<@Nullable PsiFile, RuntimeException>compute(() -> getPsiFile(e));
        if (psiFile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return performCopyTargetIdAction(
            psiFile,
            ReadAction.<@Nullable PsiElement, RuntimeException>compute(() -> getPsiElement(e)),
            getEditor(e)
        );
    }

    private CompletableFuture<Void> performCopyTargetIdAction(
        PsiFile psiFile,
        @Nullable PsiElement psiElement,
        @Nullable Editor editor
    ) {
        return calculateTargetId(psiFile, psiElement, editor).thenCompose(targetId -> {
            if (targetId == null) {
                return CompletableFuture.completedFuture(null);
            }
            // consulo: the Consulo clipboard (DataTransfer instead of TextTransferable) is written on the UI thread
            return CompletableFuture.runAsync(
                () -> {
                    CopyPasteManager clipboard = CopyPasteManager.getInstance();
                    DataTransfer transferable = DataTransfer.of(targetId);
                    clipboard.setContents(transferable);
                },
                psiFile.getProject().getUIAccess()
            );
        });
    }

    private CompletableFuture<@Nullable String> calculateTargetId(
        PsiFile psiFile,
        @Nullable PsiElement psiElement,
        @Nullable Editor editor
    ) {
        if (psiFile instanceof StarlarkFile) {
            return CompletableFuture.completedFuture(ReadAction.<@Nullable String, RuntimeException>compute(() -> {
                StarlarkCallExpression callExpression = PsiTreeUtil.getParentOfType(psiElement, StarlarkCallExpression.class);
                return callExpression != null ? calculateTargetId(callExpression) : null;
            }));
        }
        else {
            @Nullable VirtualFile virtualFile = ReadAction.<@Nullable VirtualFile, RuntimeException>compute(psiFile::getVirtualFile);
            if (virtualFile == null) {
                return CompletableFuture.completedFuture(null);
            }
            return ChooseTarget.chooseTarget(
                    TargetStorage.getTargetStorage(psiFile.getProject()).getTargetsForFile(virtualFile),
                    editor
                )
                .thenApply(label -> label != null ? BazelRepoMappingUtils.toShortString(label, psiFile.getProject()) : null);
        }
    }

    private @Nullable String calculateTargetId(StarlarkCallExpression receiver) {
        String targetName = receiver.getNameAttributeValue();
        if (targetName == null) {
            return null;
        }
        PsiFile containingFile = receiver.getContainingFile();
        VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
        if (virtualFile == null) {
            return null;
        }
        Label label = BazelRepoMappingUtils.calculateLabel(receiver.getProject(), virtualFile, targetName);
        return label != null ? BazelRepoMappingUtils.toShortString(label, receiver.getProject()) : null;
    }

    @Override
    protected void update(Project project, AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(shouldBeEnabledAndVisible(project, e));
    }

    private boolean shouldBeEnabledAndVisible(Project project, AnActionEvent e) {
        if (!ALLOWED_ACTION_PLACES.contains(e.getPlace())) {
            return false;
        }
        PsiFile file = getPsiFile(e);
        if (file == null) {
            return false;
        }
        if (file instanceof StarlarkFile starlarkFile) {
            return shouldAddActionToStarlarkFile(starlarkFile, e);
        }
        else {
            VirtualFile virtualFile = file.getVirtualFile();
            return virtualFile != null && !TargetStorage.getTargetStorage(project).getTargetsForFile(virtualFile).isEmpty();
        }
    }

    private boolean shouldAddActionToStarlarkFile(StarlarkFile file, AnActionEvent e) {
        return file.isBuildFile() && shouldAddActionToElement(getPsiElement(e));
    }

    private boolean shouldAddActionToElement(@Nullable PsiElement psiElement) {
        return psiElement != null &&
            (isValidTarget(psiElement) || isNameArgumentOfValidTarget(psiElement));
    }

    private @Nullable PsiElement getPsiElement(AnActionEvent receiver) {
        PsiFile psiFile = getPsiFile(receiver);
        Caret caret = receiver.getData(Caret.KEY);
        if (caret == null) {
            return null;
        }
        return psiFile != null ? psiFile.findElementAt(caret.getOffset()) : null;
    }

    private boolean isValidTarget(PsiElement receiver) {
        return PsiUtilCore.getElementType(receiver) == StarlarkTokenTypes.IDENTIFIER &&
            isPartOfTopLevelCallExpression(receiver);
    }

    private boolean isNameArgumentOfValidTarget(PsiElement receiver) {
        PsiElement starlarkStringLiteralExpression = receiver.getParent();
        PsiElement starlarkNamedArgumentExpression = starlarkStringLiteralExpression != null ? starlarkStringLiteralExpression.getParent() : null;
        if (starlarkNamedArgumentExpression == null) {
            return false;
        }
        return starlarkNamedArgumentExpression instanceof StarlarkNamedArgumentExpression namedArgumentExpression &&
            namedArgumentExpression.isNameArgument() &&
            isPartOfTopLevelCallExpression(namedArgumentExpression);
    }

    private boolean isPartOfTopLevelCallExpression(PsiElement receiver) {
        PsiElement starlarkReferenceExpression = receiver.getParent();
        PsiElement starlarkCallExpression = starlarkReferenceExpression != null ? starlarkReferenceExpression.getParent() : null;
        if (starlarkCallExpression == null) {
            return false;
        }
        return starlarkCallExpression instanceof StarlarkCallExpression callExpression &&
            isTopLevelCall(callExpression) &&
            callExpression.getNameAttributeValue() != null;
    }

    private boolean isTopLevelCall(PsiElement element) {
        PsiElement parent = element.getParent();
        return parent instanceof StarlarkExpressionStatement && parent.getParent() instanceof StarlarkFile;
    }
}
