package org.jetbrains.bazel.languages.projectview.elements;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;

import java.lang.reflect.Constructor;

public class ProjectViewElementType extends IElementType {
    private final Class<?>[] myParameterTypes = new Class<?>[]{ASTNode.class};
    private final Constructor<? extends PsiElement> myConstructor;

    public ProjectViewElementType(String name, Class<? extends PsiElement> psiElementClass) {
        super(name, ProjectViewLanguage.INSTANCE);
        try {
            myConstructor = psiElementClass.getConstructor(myParameterTypes);
        }
        catch (NoSuchMethodException e) {
            // consulo: Kotlin does not have checked exceptions, upstream lets NoSuchMethodException propagate
            throw new IllegalStateException(e);
        }
    }

    public PsiElement createElement(ASTNode node) {
        try {
            return myConstructor.newInstance(node);
        }
        catch (Exception e) {
            throw new IllegalStateException("No necessary constructor for " + node.getElementType(), e);
        }
    }
}
