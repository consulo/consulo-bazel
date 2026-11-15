package org.jetbrains.bazel.languages.starlark.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.ParenthesesInsertHandler;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;

import java.util.Collection;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiComment;
import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.PlatformPatterns.psiFile;

@ExtensionImpl
public class BazelGlobalFunctionCompletionContributor extends CompletionContributor {
    public BazelGlobalFunctionCompletionContributor() {
        extend(CompletionType.BASIC, globalFunctionCompletionElement(), StarlarkFunctionCompletionProvider.INSTANCE);
        extend(
            CompletionType.BASIC,
            fileSpecificFunctionCompletionElement(BazelFileType.EXTENSION),
            BazelBzlFunctionCompletionProvider.INSTANCE
        );
        extend(
            CompletionType.BASIC,
            fileSpecificFunctionCompletionElement(BazelFileType.BUILD),
            BazelBuildFunctionCompletionProvider.INSTANCE
        );
        extend(
            CompletionType.BASIC,
            fileSpecificFunctionCompletionElement(BazelFileType.MODULE),
            BazelModuleFunctionCompletionProvider.INSTANCE
        );
        extend(
            CompletionType.BASIC,
            fileSpecificFunctionCompletionElement(BazelFileType.WORKSPACE),
            BazelWorkspaceFunctionCompletionProvider.INSTANCE
        );
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    private PsiElementPattern.Capture<PsiElement> fileSpecificFunctionCompletionElement(BazelFileType bazelFileType) {
        return globalFunctionCompletionElement()
            .inFile(
                psiFile(StarlarkFile.class)
                    .with(bazelFileTypeCondition(bazelFileType))
            );
    }

    private PsiElementPattern.Capture<PsiElement> globalFunctionCompletionElement() {
        return psiElement()
            .withLanguage(StarlarkLanguage.INSTANCE)
            .withParent(StarlarkReferenceExpression.class)
            .andNot(psiComment())
            .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.DOT)));
    }

    private PatternCondition<StarlarkFile> bazelFileTypeCondition(BazelFileType bazelFileType) {
        return new PatternCondition<>("withBazelFileType") {
            @Override
            public boolean accepts(StarlarkFile file, ProcessingContext context) {
                return file.getBazelFileType() == bazelFileType;
            }
        };
    }
}

abstract class BazelFunctionCompletionProvider implements CompletionProvider {

    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        Project project = parameters.getEditor().getProject();
        if (project == null) {
            return;
        }
        if (isInStatementPosition(parameters)) {
            getFunctions(project).forEach(it -> result.addElement(functionLookupElement(it)));
            return;
        }
        getFunctions(project)
            .stream()
            .filter(it -> it.returnType() != null)
            .forEach(it -> result.addElement(functionLookupElement(it)));
    }

    private static boolean isInStatementPosition(CompletionParameters receiver) {
        PsiElement parent = receiver.getPosition().getParent();
        PsiElement grandParent = parent == null ? null : parent.getParent();
        if (grandParent instanceof StarlarkExpressionStatement) {
            return true;
        }
        else if (grandParent instanceof StarlarkCallExpression) {
            return grandParent.getParent() instanceof StarlarkExpressionStatement;
        }
        else {
            return false;
        }
    }

    protected abstract Collection<BazelGlobalFunction> getFunctions(Project project);

    private LookupElement functionLookupElement(BazelGlobalFunction function) {
        return LookupElementBuilder
            .create(function.name())
            .withInsertHandler(insertHandlerFor(function))
            .withIcon(PlatformIconGroup.nodesFunction());
    }

    private InsertHandler<LookupElement> insertHandlerFor(
        BazelGlobalFunction function
    ) {
        if (LIST_LITERAL_COMPLETION_FUNCTIONS.contains(function.name())) {
            return ListLiteralArgumentInsertHandler.INSTANCE;
        }
        else {
            return ParenthesesInsertHandler.WITH_PARAMETERS;
        }
    }

    private static final Set<String> LIST_LITERAL_COMPLETION_FUNCTIONS = Set.of("glob", "exports_files", "licenses");
}

class ListLiteralArgumentInsertHandler implements InsertHandler<LookupElement> {
    static final ListLiteralArgumentInsertHandler INSTANCE = new ListLiteralArgumentInsertHandler();

    private ListLiteralArgumentInsertHandler() {
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        Editor editor = context.getEditor();
        Document document = editor.getDocument();
        // Completion may be re-invoked over an existing call, where the parentheses are already present.
        // In that case place the caret inside them rather than inserting a second, broken `([""])`.
        CharSequence chars = document.getCharsSequence();
        int tailOffset = context.getTailOffset();
        if (tailOffset >= 0 && tailOffset < chars.length() && chars.charAt(tailOffset) == '(') {
            editor.getCaretModel().moveToOffset(context.getTailOffset() + 1);
        }
        else {
            document.insertString(context.getTailOffset(), "([\"\"])");
            editor.getCaretModel().moveToOffset(context.getTailOffset() - 3);
        }
        AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(editor);
    }
}

class StarlarkFunctionCompletionProvider extends BazelFunctionCompletionProvider {
    static final StarlarkFunctionCompletionProvider INSTANCE = new StarlarkFunctionCompletionProvider();

    @Override
    protected Collection<BazelGlobalFunction> getFunctions(Project project) {
        return BazelGlobalFunctions
            .starlarkGlobalFunctions(project)
            .values();
    }
}

class BazelBzlFunctionCompletionProvider extends BazelFunctionCompletionProvider {
    static final BazelBzlFunctionCompletionProvider INSTANCE = new BazelBzlFunctionCompletionProvider();

    @Override
    protected Collection<BazelGlobalFunction> getFunctions(Project project) {
        return BazelGlobalFunctions
            .extensionGlobalFunctions(project)
            .values();
    }
}

class BazelBuildFunctionCompletionProvider extends BazelFunctionCompletionProvider {
    static final BazelBuildFunctionCompletionProvider INSTANCE = new BazelBuildFunctionCompletionProvider();

    @Override
    protected Collection<BazelGlobalFunction> getFunctions(Project project) {
        return BazelGlobalFunctions
            .buildGlobalFunctions(project)
            .values();
    }
}

class BazelModuleFunctionCompletionProvider extends BazelFunctionCompletionProvider {
    static final BazelModuleFunctionCompletionProvider INSTANCE = new BazelModuleFunctionCompletionProvider();

    @Override
    protected Collection<BazelGlobalFunction> getFunctions(Project project) {
        return BazelGlobalFunctions
            .moduleGlobalFunctions(project)
            .values();
    }
}

class BazelWorkspaceFunctionCompletionProvider extends BazelFunctionCompletionProvider {
    static final BazelWorkspaceFunctionCompletionProvider INSTANCE = new BazelWorkspaceFunctionCompletionProvider();

    @Override
    protected Collection<BazelGlobalFunction> getFunctions(Project project) {
        return BazelGlobalFunctions
            .moduleGlobalFunctions(project)
            .values();
    }
}
