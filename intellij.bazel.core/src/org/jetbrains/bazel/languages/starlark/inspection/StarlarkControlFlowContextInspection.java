package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkBreakStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkContinueStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement;
import org.jspecify.annotations.Nullable;


@ExtensionImpl
public class StarlarkControlFlowContextInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new BreakContinueVisitor(holder);
    }

    public static class BreakContinueVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        public BreakContinueVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitBreakStatement(StarlarkBreakStatement node) {
            if (PsiTreeUtil.getParentOfType(node, StarlarkForStatement.class, true) == null) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionBreakOutsideForLoop()).range(node).create();
            }
        }

        @Override
        public void visitContinueStatement(StarlarkContinueStatement node) {
            if (PsiTreeUtil.getParentOfType(node, StarlarkForStatement.class, true) == null) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionContinueOutsideForLoop()).range(node).create();
            }
        }

        @Override
        public void visitReturnStatement(StarlarkReturnStatement node) {
            if (PsiTreeUtil.getParentOfType(node, StarlarkFunctionDeclaration.class, true) == null) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionReturnOutsideFunction()).range(node).create();
            }
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkControlFlowContext";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkControlFlowContextCheck();
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
