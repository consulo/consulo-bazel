package org.jetbrains.bazel.languages.starlark.references;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.util.function.Processor;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class BazelDepProcessor implements Processor<StarlarkElement> {
    private final String myLookingForModuleName;
    private final List<StarlarkElement> myResult = new ArrayList<>();

    BazelDepProcessor(String lookingForModuleName) {
        myLookingForModuleName = lookingForModuleName;
    }

    List<StarlarkElement> getResult() {
        return myResult;
    }

    private @Nullable String getFunctionName(StarlarkCallExpression element) {
        return element.getCalledFunctionName();
    }

    @Override
    public boolean process(StarlarkElement element) {
        if (!(element instanceof StarlarkCallExpression callExpression)) {
            return true;
        }
        if (!Objects.equals(getFunctionName(callExpression), "bazel_dep")) {
            return true;
        }
        if (Objects.equals(StarlarkFunctionCallReference.getNamedArgumentValue(callExpression, "name"), myLookingForModuleName)) {
            myResult.add(callExpression);
        }
        return true;
    }
}

public class StarlarkFunctionCallReference extends PsiReferenceBase<StarlarkReferenceExpression> {
    static @Nullable String getNamedArgumentValue(StarlarkCallExpression element, String argName) {
        StarlarkArgumentList argumentList = element.getArgumentList();
        if (argumentList == null) {
            return null;
        }
        for (StarlarkArgumentElement arg : argumentList.getArguments()) {
            if (Objects.equals(arg.getName(), argName)) {
                return arg.getValue() instanceof StarlarkStringLiteralExpression literal ? literal.getStringContents() : null;
            }
        }
        return null;
    }

    public StarlarkFunctionCallReference(StarlarkReferenceExpression element, TextRange rangeInElement) {
        super(element, rangeInElement, true);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        StarlarkReferenceExpression element = myElement;
        if (!(element.getParent() instanceof StarlarkCallExpression callExpression)) {
            return null;
        }
        String calledFunctionName = element.getText();
        switch (calledFunctionName) {
            case "archive_override", "git_override" -> {
                String moduleNameValue = getNamedArgumentValue(callExpression, "module_name");
                if (moduleNameValue == null) {
                    return null;
                }
                BazelDepProcessor processor = new BazelDepProcessor(moduleNameValue);
                if (!(element.getContainingFile() instanceof StarlarkFile file)) {
                    return null;
                }
                file.searchInFunctionCalls(processor);
                return processor.getResult().isEmpty() ? null : processor.getResult().get(0);
            }

            default -> {
                StarlarkResolveProcessor processor = new StarlarkResolveProcessor(new ArrayList<>(), element, calledFunctionName);
                SearchUtils.searchInFile(element, processor);
                return processor.getResult().isEmpty() ? null : processor.getResult().get(0);
            }
        }
    }

    @Override
    public StarlarkLookupElement[] getVariants() {
        return new StarlarkLookupElement[0];
    }

    @Override
    @RequiredWriteAction
    public PsiElement handleElementRename(String name) {
        PsiElement nameIdentifier = myElement.getNameIdentifier();
        ASTNode oldNode = nameIdentifier == null ? null : nameIdentifier.getNode();
        if (oldNode == null) {
            return myElement;
        }
        ASTNode newNode = RenameUtils.createNewName(myElement.getProject(), name);
        myElement.getNode().replaceChild(oldNode, newNode);
        return myElement;
    }
}
