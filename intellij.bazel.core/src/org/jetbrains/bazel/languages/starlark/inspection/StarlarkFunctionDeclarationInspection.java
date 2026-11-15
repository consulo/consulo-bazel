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
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jspecify.annotations.Nullable;


@ExtensionImpl
public class StarlarkFunctionDeclarationInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (ArrayUtil.contains(holder.getFile().getName(), AVAILABLE_FILE_NAMES)) {
            return new FunctionVisitor(holder);
        }
        else {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
    }

    public static class FunctionVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        public FunctionVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitFunctionDeclaration(StarlarkFunctionDeclaration node) {
            String fileName = myHolder.getFile().getName();
            LocalizeValue description;
            if (fileName.equals(Constants.MODULE_BAZEL_FILE_NAME)) {
                description = StarlarkLocalize.inspectionDescriptionFunctionDeclarationInModuleFile();
            }
            else if (ArrayUtil.contains(fileName, Constants.BUILD_FILE_NAMES)) {
                description = StarlarkLocalize.inspectionDescriptionFunctionDeclarationInBuildFile();
            }
            else {
                description = StarlarkLocalize.inspectionDescriptionFunctionDeclarationInWorkspaceFile();
            }
            myHolder.newProblem(description).range(node).create();
        }
    }

    private static final String[] AVAILABLE_FILE_NAMES = ArrayUtil.mergeArrays(Constants.BUILD_FILE_NAMES, Constants.WORKSPACE_FILE_NAMES);

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkFunctionDeclaration";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkFunctionDeclarationCheck();
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
