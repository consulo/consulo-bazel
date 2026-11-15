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
import consulo.localize.LocalizeValue;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@ExtensionImpl
public class StarlarkLoadParametersInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new DuplicateLoadSymbolsVisitor(holder);
    }

    private static class DuplicateLoadSymbolsVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        DuplicateLoadSymbolsVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitLoadStatement(StarlarkLoadStatement node) {
            Set<String> seenLocalNames = new HashSet<>();

            if (node.getLoadedFileNamePsi() == null) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadMissingFilename()).range(node).create();
                return;
            }

            List<StarlarkLoadValue> loadValues = new ArrayList<>();
            for (StarlarkElement symbol : node.getLoadedSymbolsPsi()) {
                if (symbol instanceof StarlarkLoadValue loadValue && !(loadValue instanceof StarlarkFilenameLoadValue)) {
                    loadValues.add(loadValue);
                }
            }
            if (loadValues.isEmpty()) {
                myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadMissingSymbol()).range(node).create();
                return;
            }

            for (StarlarkLoadValue value : loadValues) {
                LoadBinding binding = extractBinding(value);
                if (binding == null) {
                    continue;
                }

                if (!seenLocalNames.add(binding.name())) {
                    myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadNameDefinedMoreThanOnce())
                        .range(binding.element())
                        .create();
                }
            }
        }

        private @Nullable LoadBinding extractBinding(StarlarkLoadValue value) {
            if (value instanceof StarlarkStringLoadValue stringLoadValue) {
                StarlarkStringLiteralExpression localPsi = stringLoadValue.getLoadValueExpression();
                if (localPsi == null) {
                    return null;
                }
                String localName = localPsi.getStringContents();
                return new LoadBinding(localName, localPsi);
            }
            else if (value instanceof StarlarkNamedLoadValue namedLoadValue) {
                PsiElement nameIdentifier = namedLoadValue.getNameIdentifier();
                PsiElement localPsi = nameIdentifier != null ? nameIdentifier : namedLoadValue;
                String localName = namedLoadValue.getName();
                if (localName == null) {
                    return null;
                }
                return new LoadBinding(localName, localPsi);
            }
            else {
                return null;
            }
        }
    }

    private record LoadBinding(
        String name,
        PsiElement element
    ) {
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkLoadParameters";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkLoadParametersCheck();
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
