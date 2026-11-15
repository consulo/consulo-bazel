package org.jetbrains.bazel.languages.starlark.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.bazel.bzlmod.BazelModuleRegistryService;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl
public class BazelDepCompletionContributor extends CompletionContributor {
    public BazelDepCompletionContributor() {
        extend(
            CompletionType.BASIC,
            psiElement()
                .withLanguage(StarlarkLanguage.INSTANCE)
                .inside(StarlarkStringLiteralExpression.class)
                .inside(StarlarkCallExpression.class),
            new BazelDepCompletionProvider()
        );
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}

class BazelDepCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet result
    ) {
        StarlarkStringLiteralExpression stringLiteral =
            PsiTreeUtil.getParentOfType(parameters.getPosition(), StarlarkStringLiteralExpression.class);
        if (stringLiteral == null) {
            return;
        }
        StarlarkCallExpression callExpression = PsiTreeUtil.getParentOfType(stringLiteral, StarlarkCallExpression.class);
        if (callExpression == null) {
            return;
        }
        if (!"bazel_dep".equals(getFunctionName(callExpression))) {
            return;
        }

        Project project = parameters.getEditor().getProject();
        if (project == null) {
            return;
        }
        BazelModuleRegistryService registryService = BazelModuleRegistryService.getInstance(project);
        result.restartCompletionWhenNothingMatches();

        String argumentName = getArgumentName(stringLiteral, callExpression);
        if (argumentName == null) {
            return;
        }

        // consulo: upstream runBlockingCancellable { }, the futures are joined
        switch (argumentName) {
            case "name" -> {
                List<String> names = registryService.getModuleNames().join();
                for (String name : names) {
                    result.addElement(
                        StarlarkCompletionLookupElement.getCompletionLookupElement(name, PlatformIconGroup.nodesPackage(), 1.0)
                    );
                }
            }
            case "version" -> {
                String moduleName = getModuleName(callExpression);
                if (moduleName == null) {
                    return;
                }
                List<String> versions = registryService.getModuleVersions(moduleName).join();
                List<LookupElement> elements = new ArrayList<>(versions.size());
                for (int index = 0; index < versions.size(); index++) {
                    String v = versions.get(index);
                    elements.add(
                        StarlarkCompletionLookupElement.getCompletionLookupElement(v, PlatformIconGroup.nodesProperty(), -index)
                    );
                }
                result.addAllElements(elements);
            }
            default -> {
            }
        }
    }

    private @Nullable String getFunctionName(StarlarkCallExpression callExpression) {
        return callExpression.getCalledFunctionName();
    }

    private @Nullable String getArgumentName(StarlarkStringLiteralExpression stringLiteral, StarlarkCallExpression callExpression) {
        StarlarkArgumentList argumentList = callExpression.getArgumentList();
        if (argumentList == null) {
            return null;
        }
        StarlarkArgumentElement[] arguments = argumentList.getArguments();
        for (StarlarkArgumentElement arg : arguments) {
            PsiElement value = arg.getValue();
            if (arg.getName() != null && value != null && value.equals(stringLiteral)) {
                return arg.getName();
            }
        }
        return null;
    }

    private @Nullable String getModuleName(StarlarkCallExpression callExpression) {
        StarlarkArgumentList argumentList = callExpression.getArgumentList();
        if (argumentList == null) {
            return null;
        }
        StarlarkNamedArgumentExpression nameArg = argumentList.getKeywordArgument("name");
        if (nameArg == null) {
            return null;
        }
        if (!(nameArg.getValue() instanceof StarlarkStringLiteralExpression stringLiteral)) {
            return null;
        }
        // consulo: Kotlin removeSurrounding("\"")
        String text = stringLiteral.getText();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
