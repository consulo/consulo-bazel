package org.jetbrains.bazel.languages.starlark.psi.expressions;

import java.util.Objects;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.BazelLanguagesIcons;
import org.jetbrains.bazel.languages.starlark.StarlarkSearchScope;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * It implements {@link PsiNameIdentifierOwner} for target rule declarations in BUILD files.
 * A call expression is a target rule declaration when it has a `name` argument whose value is a simple string literal
 * and the containing file is a BUILD file.
 * It's communicated to the IntelliJ platform by
 * {@code org.jetbrains.bazel.languages.starlark.findusages.BazelRuleTargetCallElementEvaluator}.
 */
public class StarlarkCallExpression extends StarlarkBaseElement implements PsiNameIdentifierOwner {
    /**
     * Detects whether this {@link PsiElement} is a {@link StarlarkCallExpression} that is a target rule declaration in a BUILD file.
     *
     * @see StarlarkCallExpression#isRuleTarget()
     */
    public static boolean isRuleTarget(PsiElement receiver) {
        return receiver instanceof StarlarkCallExpression callExpression && callExpression.isRuleTarget();
    }

    /**
     * Whether this call expression is a top-level function call in a BUILD or MODULE.bazel file.
     */
    public static boolean isBazelFileTopLevelCall(PsiElement receiver) {
        if (!(receiver instanceof StarlarkCallExpression)) {
            return false;
        }
        // when StarlarkCallExpression is a top-level call, it is a child of StarlarkExpressionStatement,
        // which is a child of the StarlarkFile
        PsiElement parent = receiver.getParent();
        if (parent == null || !(parent.getParent() instanceof StarlarkFile file)) {
            return false;
        }
        BazelFileType fileType = file.getBazelFileType();
        return fileType == BazelFileType.BUILD || fileType == BazelFileType.MODULE;
    }

    public StarlarkCallExpression(ASTNode node) {
        super(node);
    }

    @Override
    protected void acceptVisitor(StarlarkElementVisitor visitor) {
        visitor.visitCallExpression(this);
    }

    @Override
    public SearchScope getUseScope() {
        return isRuleTarget() ? StarlarkSearchScope.starlarkProjectScope(getProject()) : super.getUseScope();
    }

    @Override
    public @Nullable String getName() {
        return isRuleTarget() ? getNameAttributeValue() : null;
    }

    @Override
    public PsiElement setName(String name) {
        if (!isRuleTarget()) {
            return this;
        }
        StarlarkStringLiteralExpression literal = nameArgumentStringLiteral();
        if (literal == null) {
            return this;
        }
        TextRange range = ElementManipulators.getValueTextRange(literal);
        PsiElement result = Objects.requireNonNull(ElementManipulators
            .getManipulator(literal))
            .handleContentChange(literal, range, name);
        return result == null ? this : result;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        if (isRuleTarget()) {
            return nameArgumentStringLiteral();
        }
        else {
            return null;
        }
    }

    @Override
    public int getTextOffset() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getTextOffset() : super.getTextOffset();
    }

    @Override
    public @Nullable ItemPresentation getPresentation() {
        if (isRuleTarget()) {
            return new BazelTargetPresentation(this);
        }
        else {
            return null;
        }
    }

    public boolean isRuleTarget() {
        if (getNameAttributeValue() == null) {
            return false;
        }
        if (!(getContainingFile() instanceof StarlarkFile file)) {
            return false;
        }
        return file.isBuildFile();
    }

    public @Nullable String getNameAttributeValue() {
        StarlarkStringLiteralExpression literal = nameArgumentStringLiteral();
        return literal == null ? null : literal.getStringContents();
    }

    public @Nullable String getCalledFunctionName() {
        return getCalledExpression() instanceof StarlarkReferenceExpression referenceExpression ? referenceExpression.getText() : null;
    }

    public @Nullable PsiElement getCalledExpression() {
        return findChildByType(StarlarkElementTypes.EXPRESSIONS);
    }

    public @Nullable List<String> getTags() {
        StarlarkArgumentList argumentList = getArgumentList();
        if (argumentList == null) {
            return null;
        }
        StarlarkNamedArgumentExpression tags = argumentList.getKeywordArgument("tags");
        if (tags == null) {
            return null;
        }
        StarlarkListLiteralExpression listLiteral = Arrays.stream(tags.getChildren())
            .filter(StarlarkListLiteralExpression.class::isInstance)
            .map(StarlarkListLiteralExpression.class::cast)
            .findFirst()
            .orElse(null);
        if (listLiteral == null) {
            return null;
        }
        return listLiteral.getElements()
            .stream()
            .filter(StarlarkStringLiteralExpression.class::isInstance)
            .map(StarlarkStringLiteralExpression.class::cast)
            .map(StarlarkStringLiteralExpression::getStringContents)
            .toList();
    }

    public @Nullable StarlarkArgumentList getArgumentList() {
        StarlarkArgumentList[] argumentLists = findChildrenByClass(StarlarkArgumentList.class);
        return argumentLists.length == 0 ? null : argumentLists[0];
    }

    // consulo: upstream PsiSymbolReference own references are merged into getReferences()
    public Collection<PsiReference> getOwnReferences() {
        StarlarkReferenceSupport support = StarlarkReferenceSupport.getInstance();
        return support == null ? List.of() : support.getOwnReferences(this);
    }

    @Override
    public PsiReference[] getReferences() {
        List<PsiReference> references = new ArrayList<>();
        PsiReference reference = getReference();
        if (reference != null) {
            references.add(reference);
        }
        references.addAll(getOwnReferences());
        return references.toArray(PsiReference.EMPTY_ARRAY);
    }

    private @Nullable StarlarkStringLiteralExpression nameArgumentStringLiteral() {
        StarlarkArgumentList argumentList = getArgumentList();
        if (argumentList == null) {
            return null;
        }
        StarlarkNamedArgumentExpression nameArgument = argumentList.getNameArgument();
        if (nameArgument == null) {
            return null;
        }
        return nameArgument.getValue() instanceof StarlarkStringLiteralExpression literal ? literal : null;
    }
}

class BazelTargetPresentation implements ItemPresentation {
    private final StarlarkCallExpression myTarget;

    BazelTargetPresentation(StarlarkCallExpression target) {
        myTarget = target;
    }

    @Override
    public LocalizeValue getPresentableText() {
        return LocalizeValue.ofNullable(myTarget.getNameAttributeValue());
    }

    // upstream overrides getIcon(boolean unused), which is deprecated in Consulo
    @Override
    public Image getIcon() {
        return BazelLanguagesIcons.bazel();
    }

    @Override
    public @Nullable String getLocationString() {
        PsiFile containingFile = myTarget.getContainingFile();
        VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        String basePath = myTarget.getProject().getBasePath();
        if (basePath == null) {
            return virtualFile.getPresentableUrl();
        }
        return StringUtil.trimStart(virtualFile.getPath(), basePath + "/");
    }
}
