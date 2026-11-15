package org.jetbrains.bazel.languages.starlark.structureView;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.language.Language;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewModelBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@ExtensionImpl
public class StarlarkStructureViewFactory implements PsiStructureViewFactory {
    @Override
    public @Nullable TreeBasedStructureViewBuilder getStructureViewBuilder(PsiFile psiFile) {
        if (!(psiFile instanceof StarlarkFile starlarkFile)) {
            return null;
        }
        return new TreeBasedStructureViewBuilder() {
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new StarlarkStructureViewModel(starlarkFile, editor);
            }
        };
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @RequiredReadAction
    static List<StarlarkTargetExpression> getAssignmentTargets(@Nullable PsiElement element) {
        if (element instanceof StarlarkTargetExpression targetExpression) {
            return List.of(targetExpression);
        }
        else if (element instanceof StarlarkTupleExpression
            || element instanceof StarlarkListLiteralExpression
            || element instanceof StarlarkParenthesizedExpression) {
            List<StarlarkTargetExpression> result = new ArrayList<>();
            for (PsiElement it : element.getChildren()) {
                result.addAll(getAssignmentTargets(it));
            }
            return result;
        }
        else {
            return List.of();
        }
    }
}

class StarlarkStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
    StarlarkStructureViewModel(StarlarkFile file, @Nullable Editor editor) {
        super(file, editor, new StarlarkFileStructureViewElement(file));
    }

    @Override
    public Sorter[] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    @RequiredReadAction
    protected boolean isSuitable(PsiElement element) {
        if (element instanceof StarlarkFunctionDeclaration functionDeclaration) {
            return functionDeclaration.isTopLevel();
        }
        else if (element instanceof StarlarkTargetExpression targetExpression) {
            return isTopLevelAssignmentTarget(targetExpression);
        }
        else if (element instanceof StarlarkCallExpression callExpression) {
            return StarlarkCallExpression.isBazelFileTopLevelCall(callExpression) && callExpression.isRuleTarget();
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return !(element instanceof StarlarkFileStructureViewElement);
    }

    @RequiredReadAction
    private boolean isTopLevelAssignmentTarget(StarlarkTargetExpression target) {
        StarlarkAssignmentStatement assignment = PsiTreeUtil.getParentOfType(target, StarlarkAssignmentStatement.class);
        if (assignment == null) {
            return false;
        }
        return assignment.isTopLevel() && StarlarkStructureViewFactory.getAssignmentTargets(assignment.leftHandSide()).contains(target);
    }
}

// presentableText is the Structure item name, locationString is shown gray next to the item name
class StarlarkFileStructureViewElement extends PsiTreeElementBase<StarlarkFile> {
    StarlarkFileStructureViewElement(StarlarkFile file) {
        super(file);
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        StarlarkFile element = getElement();
        return LocalizeValue.ofNullable(element != null ? element.getName() : null);
    }

    @Override
    @RequiredReadAction
    public @Nullable String getLocationString() {
        StarlarkFile file = getElement();
        if (file == null || !file.isBuildFile()) {
            return null;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        ResolvedLabel label = BazelRepoMappingUtils.calculateLabel(file.getProject(), virtualFile);
        return label != null ? BazelRepoMappingUtils.toShortString(label, file.getProject()) : null;
    }

    @Override
    @RequiredReadAction
    public Collection<StructureViewTreeElement> getChildrenBase() {
        StarlarkFile file = getElement();
        if (file == null) {
            return List.of();
        }
        List<StructureViewTreeElement> result = new ArrayList<>();
        for (PsiElement child : file.getChildren()) {
            if (child instanceof StarlarkFunctionDeclaration functionDeclaration) {
                result.add(new StarlarkStructureItem(functionDeclaration));
            }
            else if (child instanceof StarlarkAssignmentStatement assignmentStatement) {
                List<StarlarkTargetExpression> targets =
                    StarlarkStructureViewFactory.getAssignmentTargets(assignmentStatement.leftHandSide());
                targets.stream()
                    .sorted(Comparator.comparingInt(PsiElement::getTextOffset))
                    .forEach(it -> result.add(new StarlarkStructureItem(it)));
            }
            else if (child instanceof StarlarkExpressionStatement expressionStatement) {
                StarlarkCallExpression call = expressionStatement.callExpressionOrNull();
                if (call != null && call.isRuleTarget()) {
                    result.add(new StarlarkRuleStructureItem(call));
                }
            }
        }
        return result;
    }
}

class StarlarkStructureItem extends PsiTreeElementBase<StarlarkNamedElement> {
    StarlarkStructureItem(StarlarkNamedElement element) {
        super(element);
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        StarlarkNamedElement element = getElement();
        return LocalizeValue.ofNullable(element != null ? element.getName() : null);
    }

    @Override
    public Collection<StructureViewTreeElement> getChildrenBase() {
        return List.of();
    }
}

class StarlarkRuleStructureItem extends PsiTreeElementBase<StarlarkCallExpression> {
    StarlarkRuleStructureItem(StarlarkCallExpression call) {
        super(call);
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        StarlarkCallExpression element = getElement();
        return LocalizeValue.ofNullable(element != null ? element.getNameAttributeValue() : null);
    }

    @Override
    @RequiredReadAction
    public @Nullable String getLocationString() {
        StarlarkCallExpression element = getElement();
        return element != null ? element.getCalledFunctionName() : null;
    }

    // upstream overrides getIcon(boolean open), which is deprecated in Consulo
    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazel();
    }

    @Override
    public Collection<StructureViewTreeElement> getChildrenBase() {
        return List.of();
    }
}
