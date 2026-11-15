package org.jetbrains.bazel.languages.starlark.findusages;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProvider;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;

@ExtensionImpl
public class StarlarkUsageTypeProvider implements UsageTypeProvider {
    private static final UsageType CODE_USAGE = new UsageType(BazelPluginLocalize.findusagesStarlarkCodeUsages());
    private static final UsageType LOAD_USAGE = new UsageType(BazelPluginLocalize.findusagesStarlarkLoadUsages());

    @Override
    public UsageType getUsageType(PsiElement element) {
        if (PsiTreeUtil.getParentOfType(element, StarlarkLoadStatement.class) != null) {
            return LOAD_USAGE;
        }
        else {
            return CODE_USAGE;
        }
    }
}
