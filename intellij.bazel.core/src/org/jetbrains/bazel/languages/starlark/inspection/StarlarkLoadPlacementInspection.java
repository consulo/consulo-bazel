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
import consulo.util.collection.ArrayUtil;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jspecify.annotations.Nullable;


@ExtensionImpl
public class StarlarkLoadPlacementInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new LoadPlacementVisitor(holder);
    }

    private static class LoadPlacementVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        LoadPlacementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitLoadStatement(StarlarkLoadStatement node) {
            if (myHolder.getFile().getName().equals(Constants.MODULE_BAZEL_FILE_NAME)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadNotAllowedInModuleBazel())
                    .range(node)
                    .create();
            }
            else if (!(node.getParent() instanceof StarlarkFile)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadNotAtTopLevel())
                    .range(node)
                    .create();
            }
            else if (!ArrayUtil.contains(myHolder.getFile().getName(), CONFIG_FILE_NAMES) && hasPreviousNonLoadTopLevelStatement(node)) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadAfterStatement())
                    .range(node)
                    .create();
            }
        }

        private boolean hasPreviousNonLoadTopLevelStatement(StarlarkLoadStatement loadStatement) {
            PsiElement sibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(loadStatement);
            while (sibling != null) {
                if (!(sibling instanceof StarlarkLoadStatement) && !isStringComment(sibling)) {
                    return true;
                }
                sibling = PsiTreeUtil.skipWhitespacesAndCommentsBackward(sibling);
            }
            return false;
        }

        private boolean isStringComment(PsiElement node) {
            if (!(node instanceof StarlarkExpressionStatement stmt)) {
                return false;
            }
            PsiElement child = stmt.getFirstChild();
            if (child == null) {
                return false;
            }
            return child == stmt.getLastChild() && child instanceof StarlarkStringLiteralExpression;
        }
    }

    private static final String[] CONFIG_FILE_NAMES = ArrayUtil.mergeArrays(Constants.WORKSPACE_FILE_NAMES, Constants.BUILD_FILE_NAMES);

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkLoadPlacement";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkLoadPlacementCheck();
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
