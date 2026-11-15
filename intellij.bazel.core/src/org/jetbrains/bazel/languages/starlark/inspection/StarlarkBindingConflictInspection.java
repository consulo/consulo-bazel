package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public class StarlarkBindingConflictInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new BindingVisitor(holder);
    }

    public static class BindingVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;

        public BindingVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {
            if (!(psiFile instanceof StarlarkFile file)) {
                return;
            }
            if (ArrayUtil.contains(file.getName(), EXCLUDED_FILE_NAMES)) {
                return;
            }

            Map<String, List<DeclarationInfo>> declarationsByName = new HashMap<>();

            for (PsiElement child : file.getChildren()) {
                if (child instanceof StarlarkFunctionDeclaration functionDeclaration) {
                    String name = functionDeclaration.getName();
                    if (name == null) {
                        continue;
                    }
                    PsiElement nameIdentifier = functionDeclaration.getNameIdentifier();
                    PsiElement element = nameIdentifier != null ? nameIdentifier : functionDeclaration;
                    collectDeclarationInfo(name, element, BindingScope.GLOBAL, declarationsByName);
                }
                else if (child instanceof StarlarkAssignmentStatement assignmentStatement) {
                    if (!assignmentStatement.isTopLevel()) {
                        continue;
                    }
                    String name = assignmentStatement.getName();
                    if (name == null) {
                        continue;
                    }
                    collectDeclarationInfo(name, assignmentStatement, BindingScope.GLOBAL, declarationsByName);
                }
                else if (child instanceof StarlarkLoadStatement loadStatement) {
                    for (StarlarkElement symbol : loadStatement.getLoadedSymbolsPsi()) {
                        String name;
                        if (symbol instanceof StarlarkStringLoadValue stringLoadValue) {
                            StarlarkStringLiteralExpression loadValueExpression = stringLoadValue.getLoadValueExpression();
                            name = loadValueExpression == null ? null : loadValueExpression.getStringContents();
                        }
                        else {
                            name = symbol.getName();
                        }
                        if (name == null) {
                            continue;
                        }
                        collectDeclarationInfo(name, symbol, BindingScope.FILE_LOCAL, declarationsByName);
                    }
                }
            }

            for (Map.Entry<String, List<DeclarationInfo>> entry : declarationsByName.entrySet()) {
                String name = entry.getKey();
                List<DeclarationInfo> declarations = entry.getValue();
                if (declarations.size() <= 1) {
                    continue;
                }

                Set<NavigatablePsiElement> distinctNavigatableDeclarations = new LinkedHashSet<>();
                for (DeclarationInfo declaration : declarations) {
                    NavigatablePsiElement navigatable = declaration.navigatable();
                    if (navigatable != null) {
                        distinctNavigatableDeclarations.add(navigatable);
                    }
                }
                List<NavigatablePsiElement> allNavigatableDeclarations = new ArrayList<>(distinctNavigatableDeclarations);
                boolean crossKindConflict = declarations.stream().anyMatch(it -> it.scope() == BindingScope.GLOBAL) &&
                    declarations.stream().anyMatch(it -> it.scope() == BindingScope.FILE_LOCAL);
                boolean seenGlobal = false;
                boolean seenFileLocal = false;

                for (DeclarationInfo declaration : declarations) {
                    PsiElement element = declaration.element();
                    BindingScope scope = declaration.scope();
                    NavigatablePsiElement navigatable = declaration.navigatable();
                    boolean first = !seenGlobal && !seenFileLocal;
                    boolean sameScopeConflict = scope == BindingScope.GLOBAL ? seenGlobal : seenFileLocal;

                    LocalizeValue message;
                    if (first && !crossKindConflict) {
                        // consulo: upstream passed name, but the message has no placeholder
                        message = StarlarkLocalize.inspectionDescriptionBindingConflictOriginal();
                    }
                    else if (sameScopeConflict) {
                        message = StarlarkLocalize.inspectionDescriptionBindingConflictSameKind(name);
                    }
                    else {
                        message = StarlarkLocalize.inspectionDescriptionBindingConflictCrossKind(name, scope.getQualifier());
                    }

                    // consulo: upstream wrapped the ModCommand action with LocalQuickFix.from(...)
                    LocalQuickFix fix = navigatable == null
                        ? null
                        : new ShowConflictingDeclarationsAction(name, navigatable, allNavigatableDeclarations);

                    registerProblem(element, message, fix);

                    if (scope == BindingScope.GLOBAL) {
                        seenGlobal = true;
                    }
                    else {
                        seenFileLocal = true;
                    }
                }
            }
        }

        private void collectDeclarationInfo(
            String name,
            PsiElement elem,
            BindingScope scope,
            Map<String, List<DeclarationInfo>> declarationsByName
        ) {
            NavigatablePsiElement navigatable;
            if (elem instanceof NavigatablePsiElement navigatableElem) {
                navigatable = navigatableElem;
            }
            else if (elem.getParent() instanceof NavigatablePsiElement navigatableParent) {
                navigatable = navigatableParent;
            }
            else if (elem.getNavigationElement() instanceof NavigatablePsiElement navigatableNavigationElement) {
                navigatable = navigatableNavigationElement;
            }
            else {
                navigatable = null;
            }

            declarationsByName.computeIfAbsent(name, it -> new ArrayList<>()).add(new DeclarationInfo(elem, scope, navigatable));
        }

        private void registerProblem(PsiElement element, LocalizeValue message, @Nullable LocalQuickFix fix) {
            if (fix == null) {
                myHolder.newProblem(message).range(element).create();
            }
            else {
                myHolder.newProblem(message).range(element).withFix(fix).create();
            }
        }
    }

    // consulo: the ModCommand API (PsiBasedModCommandAction, ModCommand.chooseAction) does not exist in Consulo; a classic
    // quick fix (not in a write action) that shows the same choice as a list popup. The intention preview (generatePreview) is dropped.
    public static class ShowConflictingDeclarationsAction extends LocalQuickFixAndIntentionActionOnPsiElement {
        private final String myDeclarationName;
        private final List<SmartPsiElementPointer<NavigatablePsiElement>> myConflictingDeclarationsPointers;

        public ShowConflictingDeclarationsAction(
            String declarationName,
            NavigatablePsiElement originalDeclaration,
            List<NavigatablePsiElement> conflictingDeclarations
        ) {
            super(originalDeclaration);
            myDeclarationName = declarationName;
            myConflictingDeclarationsPointers = conflictingDeclarations.stream().map(SmartPointerManager::createPointer).toList();
        }

        private List<NavigatablePsiElement> getConflictingDeclarations() {
            List<NavigatablePsiElement> result = new ArrayList<>();
            for (SmartPsiElementPointer<NavigatablePsiElement> pointer : myConflictingDeclarationsPointers) {
                NavigatablePsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }

        // consulo: Consulo intention actions have no family name; kept from upstream
        public LocalizeValue getFamilyName() {
            return StarlarkLocalize.inspectionQuickfixShowConflictingDeclarationsFamily();
        }

        // consulo: upstream getPresentation(context, element) returned Presentation.of(text)
        @Override
        public LocalizeValue getText() {
            return StarlarkLocalize.inspectionQuickfixShowConflictingDeclarationsText(myDeclarationName);
        }

        // consulo: upstream perform(context, element) returned ModCommand.chooseAction(title, navigateActions)
        @Override
        public void invoke(
            Project project,
            PsiFile file,
            @Nullable Editor editor,
            PsiElement startElement,
            PsiElement endElement
        ) {
            if (!(startElement instanceof NavigatablePsiElement element)) {
                return;
            }
            List<NavigateToConflictingDeclarationAction> navigateActions = new ArrayList<>();
            for (NavigatablePsiElement conflictingDeclaration : getConflictingDeclarations()) {
                if (!conflictingDeclaration.equals(element)) {
                    navigateActions.add(new NavigateToConflictingDeclarationAction(conflictingDeclaration));
                }
            }
            navigateActions.add(new NavigateToConflictingDeclarationAction(element, true));

            BaseListPopupStep<NavigateToConflictingDeclarationAction> step = new BaseListPopupStep<>(
                StarlarkLocalize.inspectionQuickfixShowConflictingDeclarationsPopupTitle().get(),
                navigateActions
            ) {
                @Override
                public String getTextFor(NavigateToConflictingDeclarationAction value) {
                    return value.getText().get();
                }

                @Override
                public @Nullable Image getIconFor(NavigateToConflictingDeclarationAction value) {
                    return value.getIcon();
                }

                @Override
                public @Nullable PopupStep onChosen(NavigateToConflictingDeclarationAction selectedValue, boolean finalChoice) {
                    return doFinalStep(() -> selectedValue.perform(project));
                }
            };
            ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, step);
            Editor popupEditor = editor != null ? editor : FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (popupEditor != null) {
                EditorPopupHelper.getInstance().showPopupInBestPositionFor(popupEditor, popup);
            }
            else {
                popup.showInFocusCenter();
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }

    // consulo: upstream is a PsiBasedModCommandAction used as an item of ModCommand.chooseAction; here it is a plain item of the
    // list popup shown by ShowConflictingDeclarationsAction. The intention preview (generatePreview) is dropped.
    public static class NavigateToConflictingDeclarationAction {
        private final SmartPsiElementPointer<NavigatablePsiElement> myDeclarationPointer;
        private final boolean myIsOriginalDeclaration;

        public NavigateToConflictingDeclarationAction(NavigatablePsiElement declaration) {
            this(declaration, false);
        }

        public NavigateToConflictingDeclarationAction(NavigatablePsiElement declaration, boolean isOriginalDeclaration) {
            myDeclarationPointer = SmartPointerManager.createPointer(declaration);
            myIsOriginalDeclaration = isOriginalDeclaration;
        }

        public LocalizeValue getFamilyName() {
            return StarlarkLocalize.inspectionQuickfixShowConflictingDeclarationsNavigateFamily();
        }

        // consulo: upstream getPresentation(context, element): Presentation.of(title).withIcon(...); the highlighting of the
        // element range (withHighlighting) has no list popup counterpart
        public LocalizeValue getText() {
            NavigatablePsiElement element = myDeclarationPointer.getElement();
            if (element == null) {
                return getFamilyName();
            }
            return StarlarkLocalize.inspectionQuickfixShowConflictingDeclarationsNavigateText(
                Objects.requireNonNull(element.getContainingFile()).getName(), myIsOriginalDeclaration ? 1 : 0
            );
        }

        public @Nullable Image getIcon() {
            NavigatablePsiElement element = myDeclarationPointer.getElement();
            if (element == null) {
                return null;
            }
            return IconDescriptorUpdaters.getIcon(Objects.requireNonNull(element.getContainingFile()), 0);
        }

        // consulo: upstream perform(context, element) returned ModCommand.select(target); opens the file and selects the target
        public void perform(Project project) {
            NavigatablePsiElement element = myDeclarationPointer.getElement();
            if (element == null || !element.isValid()) {
                return;
            }
            PsiElement nameIdentifier = element instanceof PsiNameIdentifierOwner nameIdentifierOwner
                ? nameIdentifierOwner.getNameIdentifier()
                : null;
            PsiElement target = nameIdentifier != null ? nameIdentifier : element;
            VirtualFile virtualFile = Objects.requireNonNull(target.getContainingFile()).getVirtualFile();
            if (virtualFile == null) {
                return;
            }
            TextRange range = target.getTextRange();
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(
                OpenFileDescriptorFactory.getInstance(project).newBuilder(virtualFile).offset(range.getStartOffset()).build(),
                true
            );
            if (editor != null) {
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
            }
        }
    }

    private static final String[] EXCLUDED_FILE_NAMES = ArrayUtil.mergeArrays(
        Constants.BUILD_FILE_NAMES,
        Constants.WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod"
    );

    private enum BindingScope {
        GLOBAL("global"),
        FILE_LOCAL("file-local");

        private final String myQualifier;

        BindingScope(String qualifier) {
            myQualifier = qualifier;
        }

        public String getQualifier() {
            return myQualifier;
        }
    }

    private record DeclarationInfo(
        PsiElement element,
        BindingScope scope,
        @Nullable NavigatablePsiElement navigatable
    ) {
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkBindingConflict";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkBindingConflictCheck();
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
