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
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFileLoadedSymbols;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkFileLoadedSymbols.StarlarkLoadedSymbol;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkMutationUtils;
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStaticValueUtils.StarlarkStaticValueKind;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
public class StarlarkFrozenLoadedValueMutationInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new FrozenLoadedValueMutationVisitor(holder);
    }

    private static class FrozenLoadedValueMutationVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        FrozenLoadedValueMutationVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {
            if (!(psiFile instanceof StarlarkFile file)) {
                return;
            }
            StarlarkFileLoadedSymbols loadedSymbols = StarlarkFileLoadedSymbols.create(file);
            if (loadedSymbols.isEmpty()) {
                return;
            }

            Set<PsiElement> reported = new HashSet<>();

            PsiTreeUtil.processElements(file, it -> {
                StarlarkMutationUtils.Mutation mutation = StarlarkMutationUtils.mutationOrNull(it);
                if (mutation == null) {
                    return true;
                }
                StarlarkLoadedSymbol loadedSymbol = loadedSymbols.findByReference(mutation.target());
                if (loadedSymbol == null) {
                    return true;
                }
                LocalizeValue problemDescription = mutationProblemDescription(mutation, loadedSymbol);
                if (problemDescription == null) {
                    return true;
                }

                if (reported.add(mutation.problemElement())) {
                    myHolder.newProblem(problemDescription).range(mutation.problemElement()).create();
                }

                return true;
            });
        }

        private @Nullable LocalizeValue mutationProblemDescription(
            StarlarkMutationUtils.Mutation mutation,
            StarlarkLoadedSymbol loadedSymbol
        ) {
            switch (mutation.kind()) {
                case SUBSCRIPTION_ASSIGNMENT:
                case AUGMENTED_ASSIGNMENT:
                    return StarlarkLocalize.inspectionDescriptionLoadedValueMutation(loadedSymbol.getLocalName());

                case METHOD_CALL: {
                    String methodName = mutation.methodName();
                    if (methodName == null) {
                        return null;
                    }
                    StarlarkStaticValueKind valueKind = loadedSymbol.getStaticValueKind();
                    if (valueKind == null) {
                        return null;
                    }
                    if (!valueKind.isMutatedBy(methodName)) {
                        return null;
                    }
                    return StarlarkLocalize.inspectionDescriptionLoadedValueMutation(loadedSymbol.getLocalName());
                }
                default:
                    throw new IllegalStateException("Unexpected mutation kind: " + mutation.kind());
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
        return "StarlarkFrozenLoadedValueMutation";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkFrozenLoadedValueMutationCheck();
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
