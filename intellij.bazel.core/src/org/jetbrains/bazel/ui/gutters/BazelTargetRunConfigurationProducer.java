package org.jetbrains.bazel.ui.gutters;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
class BazelRunConfigurationFromTargetProducer extends BazelRunConfigurationProducer {
    public BazelRunConfigurationFromTargetProducer() {
    }

    @Override
    public @Nullable GutterAction getGutterAction(
        PsiElement element,
        BuildTarget target
    ) {
        if (element instanceof PsiBazelTarget) {
            return new GutterAction();
        }
        else {
            return null;
        }
    }
}
