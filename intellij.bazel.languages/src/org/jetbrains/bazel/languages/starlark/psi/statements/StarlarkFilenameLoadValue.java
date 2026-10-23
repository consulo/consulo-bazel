package org.jetbrains.bazel.languages.starlark.psi.statements;

import consulo.component.util.Iconable;
import consulo.language.ast.ASTNode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jspecify.annotations.Nullable;

public class StarlarkFilenameLoadValue extends StarlarkBaseElement implements StarlarkLoadValue, Iconable {
    public StarlarkFilenameLoadValue(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitFilenameLoadValue(this);
    }

    @Override
    public @Nullable Image getIcon(int flags) {
        return PlatformIconGroup.actionsImport();
    }
}
