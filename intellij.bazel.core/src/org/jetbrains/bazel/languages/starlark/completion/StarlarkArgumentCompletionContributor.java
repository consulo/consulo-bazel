package org.jetbrains.bazel.languages.starlark.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunction;
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions.BazelGlobalFunctionParameter;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class StarlarkArgumentCompletionContributor extends CompletionContributor {
    @SuppressWarnings("unchecked")
    public StarlarkArgumentCompletionContributor() {
        extend(
            CompletionType.BASIC,
            psiElement().withParents(StarlarkReferenceExpression.class, StarlarkArgumentExpression.class),
            StarlarkArgumentCompletionProvider.INSTANCE
        );
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}

class StarlarkArgumentCompletionProvider implements CompletionProvider {
    static final StarlarkArgumentCompletionProvider INSTANCE = new StarlarkArgumentCompletionProvider();

    private StarlarkArgumentCompletionProvider() {
    }

    private Map<String, BazelGlobalFunction> fileTypeToGlobalFunctions(StarlarkFile file) {
        Project project = file.getProject();
        Map<String, BazelGlobalFunction> result = new LinkedHashMap<>(
            switch (file.getBazelFileType()) {
                case EXTENSION -> BazelGlobalFunctions.extensionGlobalFunctions(project);
                case BUILD -> BazelGlobalFunctions.buildGlobalFunctions(project);
                case MODULE -> BazelGlobalFunctions.moduleGlobalFunctions(project);
                case WORKSPACE -> BazelGlobalFunctions.moduleGlobalFunctions(project);
            }
        );
        result.putAll(BazelGlobalFunctions.starlarkGlobalFunctions(project));
        return result;
    }

    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        PsiFile originalFile = parameters.getOriginalFile();
        if (!(originalFile instanceof StarlarkFile file)) {
            return;
        }
        StarlarkCallExpression starlarkCallExpression = PsiTreeUtil.getParentOfType(parameters.getPosition(), StarlarkCallExpression.class);
        PsiElement firstChild = starlarkCallExpression == null ? null : starlarkCallExpression.getFirstChild();
        if (starlarkCallExpression == null || firstChild == null) {
            return;
        }
        String functionName = firstChild.getText();
        BazelGlobalFunction globalFunction = fileTypeToGlobalFunctions(file).get(functionName);

        if (globalFunction != null) {
            Set<String> argumentList =
                ((StarlarkArgumentList) Objects.requireNonNull(starlarkCallExpression.getLastChild())).getArgumentNames();
            addCompletionForGlobalFunction(result, globalFunction, argumentList);
        }
        else {
            StarlarkArgumentExpression argument = PsiTreeUtil.getParentOfType(parameters.getPosition(), StarlarkArgumentExpression.class);
            if (argument == null) {
                return;
            }
            PsiReference reference = argument.getReference();
            if (reference == null) {
                return;
            }
            for (Object variant : reference.getVariants()) {
                if (variant instanceof StarlarkNamedLookupElement namedLookupElement) {
                    result.addElement(namedLookupElement);
                }
            }
        }
    }

    private void addCompletionForGlobalFunction(
        CompletionResultSet result,
        BazelGlobalFunction function,
        Set<String> argumentList
    ) {
        List<BazelGlobalFunctionParameter> filtered =
            function.params().stream().filter(it ->
                !argumentList.contains(it.name())
            ).toList();

        filtered.forEach(it ->
            result.addElement(PrioritizedLookupElement.withPriority(argumentLookupElement(it), 1.0))
        );
    }

    private static class ArgumentInsertHandler<T extends LookupElement> implements InsertHandler<T> {
        private final String myDefault;

        ArgumentInsertHandler(String defaultValue) {
            myDefault = defaultValue;
        }

        public String getDefault() {
            return myDefault;
        }

        @Override
        public void handleInsert(InsertionContext context, T item) {
            Editor editor = context.getEditor();
            Document document = editor.getDocument();
            if (myDefault.equals("\'\'") || myDefault.equals("\"\"") || myDefault.equals("[]") || myDefault.equals("{}")) {
                document.insertString(context.getTailOffset(), " = " + myDefault + ",");
                editor.getCaretModel().moveToOffset(context.getTailOffset() - 2);
            }
            else {
                document.insertString(context.getTailOffset(), " = ,");
                editor.getCaretModel().moveToOffset(context.getTailOffset() - 1);
            }
        }
    }

    private LookupElement argumentLookupElement(BazelGlobalFunctionParameter arg) {
        LookupElementBuilder it = LookupElementBuilder
            .create(arg.name())
            .withIcon(PlatformIconGroup.nodesParameter());
        // consulo: upstream letIf(arg.named) { } from BazelrcCompletionContributor
        if (arg.named()) {
            String defaultValue = arg.defaultValue();
            return it.withInsertHandler(new ArgumentInsertHandler<>(defaultValue != null ? defaultValue : ""));
        }
        return it;
    }
}
