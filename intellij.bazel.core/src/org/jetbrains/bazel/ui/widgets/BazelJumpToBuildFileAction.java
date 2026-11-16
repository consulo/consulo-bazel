package org.jetbrains.bazel.ui.widgets;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.action.SuspendableAction;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class BazelJumpToBuildFileAction extends SuspendableAction {
    protected BazelJumpToBuildFileAction() {
        super(BazelPluginLocalize.widgetOpenBuildFile(), PlatformIconGroup.generalOpennewtab());
    }

    protected abstract CompletableFuture<@Nullable Label> getTargetLabel(Project project, AnActionEvent e);

    @Override
    protected CompletableFuture<Void> actionPerformed(Project project, AnActionEvent e) {
        return getTargetLabel(project, e).thenCompose(target -> {
            if (target == null) {
                return CompletableFuture.completedFuture(null);
            }
            return jumpToBuildFile(project, target);
        });
    }

    // it is registered in plugin.xml
    // consulo: the registered action is the top-level BazelJumpToBuildFileActionXmlRegistered (@ActionImpl on nested classes is not supported)
    public static class XmlRegistered extends BazelJumpToBuildFileAction {
        @Override
        protected CompletableFuture<@Nullable Label> getTargetLabel(Project project, AnActionEvent e) {
            // consulo: readAction { } -> blocking read action
            @Nullable VirtualFile virtualFile = ReadAction.<@Nullable VirtualFile, RuntimeException>compute(() -> {
                PsiFile psiFile = getPsiFile(e);
                return psiFile != null ? psiFile.getVirtualFile() : null;
            });
            if (virtualFile == null) {
                return CompletableFuture.completedFuture(null);
            }
            return ChooseTarget.chooseTarget(TargetStorage.getTargetStorage(project).getTargetsForFile(virtualFile), getEditor(e));
        }

        @Override
        protected void update(Project project, AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(shouldBeEnabledAndVisible(project, e));
        }

        private boolean shouldBeEnabledAndVisible(Project project, AnActionEvent e) {
            PsiFile psiFile = getPsiFile(e);
            VirtualFile virtualFile = psiFile != null ? psiFile.getVirtualFile() : null;
            return virtualFile != null && !TargetStorage.getTargetStorage(project).getTargetsForFile(virtualFile).isEmpty();
        }
    }

    public static class NonXmlRegistered extends BazelJumpToBuildFileAction {
        private final Supplier<@Nullable BuildTargetInfo> myGetTarget;

        public NonXmlRegistered(Supplier<@Nullable BuildTargetInfo> getTarget) {
            myGetTarget = getTarget;
        }

        @Override
        protected CompletableFuture<@Nullable Label> getTargetLabel(Project project, AnActionEvent e) {
            BuildTargetInfo target = myGetTarget.get();
            return CompletableFuture.completedFuture(target != null ? target.id() : null);
        }

        public void registerShortcut(JComponent component) {
            ShortcutSet shortcutSet = KeymapUtil.getActiveKeymapShortcuts("EditSource");
            registerCustomShortcutSet(shortcutSet, component);
        }
    }

    // consulo: the coroutine has no input, Consulo coroutines are started with a null input
    @SuppressWarnings("NullAway")
    public static CompletableFuture<Void> jumpToBuildFile(Project project, Label target) {
        // consulo: readAction { } + withContext(Dispatchers.EDT) { } -> ReadLock + UIAction steps
        return Coroutine
            .first(ReadLock.<@Nullable Object, @Nullable PsiElement>apply(ignored -> findDefinition(project, target)))
            .then(UIAction.<@Nullable PsiElement, @Nullable Void>apply(definition -> {
                if (definition != null) {
                    // consulo: EditorHelper.openInEditor has no focusEditor parameter
                    EditorHelper.openInEditor(definition, true);
                }
                return null;
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture();
    }

    @RequiredReadAction
    private static @Nullable PsiElement findDefinition(Project project, Label target) {
        PsiElement file = LabelResolveUtils.resolveLabel(project, target, null, false);
        if (!(file instanceof StarlarkFile starlarkFile)) {
            return file;
        }

        PsiElement definition = findDefinitionByLongestPrefix(starlarkFile, target);
        return definition != null ? definition : starlarkFile;
    }

    /**
     * Uses a simple longest prefix heuristic to find the corresponding definition
     * inside the BUILD file. Similar to what the Google plugin does.
     */
    private static @Nullable PsiElement findDefinitionByLongestPrefix(StarlarkFile file, Label target) {
        String name = target.getTargetName();

        PsiElement candidate = null;
        int candidateScore = 0;

        for (StarlarkExpressionStatement expr : file.findChildrenByClass(StarlarkExpressionStatement.class)) {
            int score = getCallCommonPrefix(expr, name);

            if (score > candidateScore) {
                candidate = expr;
                candidateScore = score;
            }
        }

        return candidate;
    }

    private static int getCallCommonPrefix(StarlarkExpressionStatement expr, String value) {
        String name = getCallName(expr);
        if (name == null) {
            return 0;
        }
        return StringUtil.commonPrefixLength(name, value);
    }

    private static @Nullable String getCallName(StarlarkExpressionStatement expr) {
        StarlarkCallExpression call = expr.callExpressionOrNull();
        if (call == null) {
            return null;
        }
        StarlarkArgumentList args = call.getArgumentList();
        if (args == null) {
            return null;
        }
        StarlarkNamedArgumentExpression name = args.getKeywordArgument("name");
        if (name == null) {
            return null;
        }
        return name.getArgumentStringValue();
    }
}
