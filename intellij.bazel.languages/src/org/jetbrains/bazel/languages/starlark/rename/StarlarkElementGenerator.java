package org.jetbrains.bazel.languages.starlark.rename;

import java.util.Objects;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes;
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes;

public class StarlarkElementGenerator {
    private static final String DUMMY_FILENAME = "dummy.bzl";

    private final Project myProject;

    public StarlarkElementGenerator(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    public ASTNode createTokenType(String tokenString) {
        return Objects.requireNonNull(createDummyFile(tokenString).getNode().getFirstChildNode());
    }

    public ASTNode createNameIdentifier(String name) {
        PsiFile dummyFile = createDummyFile(name);
        ASTNode referenceNode = Objects.requireNonNull(Objects.requireNonNull(dummyFile.getNode().getFirstChildNode()).getFirstChildNode());
        ASTNode nameNode = Objects.requireNonNull(referenceNode.getFirstChildNode());
        if (nameNode.getElementType() != StarlarkTokenTypes.IDENTIFIER) {
            throw new IllegalStateException("Expected elementType to be IDENTIFIER while creating dummy file");
        }
        return nameNode;
    }

    public ASTNode createStringLiteral(String contents) {
        PsiFile dummyFile = createDummyFile("\"" + contents + "\"");
        ASTNode stringNode = Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(dummyFile.getNode().getFirstChildNode()).getFirstChildNode()).getFirstChildNode());
        if (stringNode.getElementType() != StarlarkTokenTypes.STRING) {
            throw new IllegalStateException("Expected elementType to be STRING while creating dummy file");
        }
        return stringNode;
    }

    public ASTNode createStringLiteralExpression(String contents) {
        PsiFile dummyFile = createDummyFile("\"" + contents + "\"");
        ASTNode stringNode = Objects.requireNonNull(Objects.requireNonNull(dummyFile.getNode().getFirstChildNode()).getFirstChildNode());
        if (stringNode.getElementType() != StarlarkElementTypes.STRING_LITERAL_EXPRESSION) {
            throw new IllegalStateException("Expected elementType to be STRING_LITERAL_EXPRESSION");
        }
        return stringNode;
    }

    /**
     * You want to be passing contents sort of in the form
     * {@code deps = [],}
     */
    public ASTNode createNamedArgument(String contents) {
        PsiFile dummyFile = createDummyFile("dummy(" + contents + ")");
        ASTNode argumentNode =
            Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(dummyFile.getNode().getFirstChildNode()).getFirstChildNode()).getLastChildNode())
                .getChildren(null)[1];
        if (argumentNode.getElementType() != StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION) {
            throw new IllegalStateException("Expected elementType to be NAMED_ARGUMENT_EXPRESSION");
        }
        return argumentNode;
    }

    private PsiFile createDummyFile(String contents) {
        PsiFileFactory factory = PsiFileFactory.getInstance(myProject);
        // consulo: upstream used PsiFileFactoryImpl.trySetupPsiForFile(LightVirtualFile(DUMMY_FILENAME, StarlarkFileType, contents),
        // StarlarkLanguage, false, true) and failed with "Failed to create dummy file" on null
        return factory.createFileFromText(DUMMY_FILENAME, StarlarkLanguage.INSTANCE, contents, false, true);
    }
}
