package org.jetbrains.bazel.languages.bazelrc.quickfix;

import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.languages.bazelrc.flags.Flag;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcElementGenerator;
import org.jspecify.annotations.Nullable;

// consulo: the ModCommand API (PsiUpdateModCommandAction) does not exist in Consulo; a classic intention action bound to the
// element (smart pointer), run in a write action
public class RenameFlagNameFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final Flag myFlag;

    public RenameFlagNameFix(PsiElement element, Flag flag) {
        super(element);
        myFlag = flag;
    }

    public Flag getFlag() {
        return myFlag;
    }

    // consulo: upstream invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater)
    @Override
    public void invoke(
        Project project,
        PsiFile psiFile,
        @Nullable Editor editor,
        PsiElement element,
        PsiElement endElement
    ) {
        String target;
        if (element.getText().startsWith("--no")) {
            target = "no" + myFlag.getOption().name();
        }
        else {
            target = myFlag.getOption().name();
        }

        element.replace(new BazelrcElementGenerator(project).createFlagName(target));
    }

    // consulo: upstream getPresentation(context, element) returned Presentation.of(text)
    @Override
    public LocalizeValue getText() {
        return BazelPluginLocalize.quickfixBazelrcRenameFlagPresentation(myFlag.getOption().name());
    }

    // consulo: Consulo intention actions have no family name; kept from upstream
    public LocalizeValue getFamilyName() {
        return BazelPluginLocalize.quickfixBazelrcRenameFlagDescription();
    }
}
