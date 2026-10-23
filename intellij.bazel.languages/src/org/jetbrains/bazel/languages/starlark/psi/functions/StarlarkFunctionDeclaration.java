package org.jetbrains.bazel.languages.starlark.psi.functions;

import consulo.component.util.Iconable;
import consulo.content.scope.SearchScope;
import consulo.language.ast.ASTNode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class StarlarkFunctionDeclaration extends StarlarkNamedElement implements StarlarkCallable, Iconable {
    public StarlarkFunctionDeclaration(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitFunctionDeclaration(this);
    }

    @Override
    public @Nullable Image getIcon(int flags) {
        return PlatformIconGroup.nodesFunction();
    }

    @Override
    public SearchScope getUseScope() {
        if (isTopLevel() && !isFilePrivate()) {
            return StarlarkSearchScope.starlarkProjectScope(getProject());
        }
        else {
            return super.getUseScope();
        }
    }

    public boolean isTopLevel() {
        return getParent() instanceof StarlarkFile;
    }

    @Override
    public List<StarlarkParameter> getNamedParameters() {
        return getParameters().stream()
            .filter(it -> it instanceof StarlarkMandatoryParameter || it instanceof StarlarkOptionalParameter)
            .toList();
    }

    @Override
    public List<StarlarkParameter> getParameters() {
        StarlarkParameterList parameterList = findChildByClass(StarlarkParameterList.class);
        return parameterList != null ? parameterList.getParameters() : List.of();
    }

    public StarlarkStatementList getStatementList() {
        return Objects.requireNonNull(findChildByClass(StarlarkStatementList.class));
    }

    private boolean isFilePrivate() {
        String name = getName();
        return name != null && name.startsWith("_");
    }
}
