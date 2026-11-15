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
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;


@ExtensionImpl
public class StarlarkLoadPrivateSymbolInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new LoadPrivateSymbolVisitor(holder);
    }

    public static class LoadPrivateSymbolVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        public LoadPrivateSymbolVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitStringLoadValue(StarlarkStringLoadValue node) {
            String name = node.getLoadValueExpressionContent();
            if (name == null) {
                return;
            }
            if (!name.isEmpty() && name.charAt(0) == '_') {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadPrivateSymbol())
                    .range(node)
                    .create();
            }
        }

        @Override
        public void visitNamedLoadValue(StarlarkNamedLoadValue node) {
            String name = node.getLoadValueExpressionContent();
            if (name == null) {
                return;
            }
            if (!name.isEmpty() && name.charAt(0) == '_') {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadPrivateSymbol())
                    .range(node)
                    .create();
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
        return "StarlarkLoadPrivateSymbol";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkLoadPrivateSymbolCheck();
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
