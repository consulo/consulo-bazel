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
import consulo.util.collection.ArrayUtil;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement;
import org.jspecify.annotations.Nullable;


@ExtensionImpl
public class StarlarkStatementContainerPlacementInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new StatementContainerVisitor(holder);
    }

    public static class StatementContainerVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        public StatementContainerVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitForStatement(StarlarkForStatement node) {
            if (node.getParent() instanceof StarlarkFile) {
                String fileName = myHolder.getFile().getName();
                LocalizeValue description;
                if (ArrayUtil.contains(fileName, Constants.BUILD_FILE_NAMES)) {
                    description = StarlarkLocalize.inspectionDescriptionForPlacementBuild();
                }
                else if (ArrayUtil.contains(fileName, WORKSPACE_ONLY_FILE_NAMES)) {
                    description = StarlarkLocalize.inspectionDescriptionForPlacementWorkspace();
                }
                else {
                    description = StarlarkLocalize.inspectionDescriptionForPlacement();
                }
                myHolder.newProblem(description).range(node).create();
            }
        }

        @Override
        public void visitIfStatement(StarlarkIfStatement node) {
            if (node.getParent() instanceof StarlarkFile) {
                String fileName = myHolder.getFile().getName();
                LocalizeValue description;
                if (ArrayUtil.contains(fileName, Constants.BUILD_FILE_NAMES)) {
                    description = StarlarkLocalize.inspectionDescriptionIfPlacementBuild();
                }
                else if (ArrayUtil.contains(fileName, WORKSPACE_ONLY_FILE_NAMES)) {
                    description = StarlarkLocalize.inspectionDescriptionIfPlacementWorkspace();
                }
                else {
                    description = StarlarkLocalize.inspectionDescriptionIfPlacement();
                }
                myHolder.newProblem(description).range(node).create();
            }
        }
    }

    private static final String[] WORKSPACE_ONLY_FILE_NAMES = {Constants.WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod"};

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkStatementContainerPlacement";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkStatementContainerPlacementCheck();
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
