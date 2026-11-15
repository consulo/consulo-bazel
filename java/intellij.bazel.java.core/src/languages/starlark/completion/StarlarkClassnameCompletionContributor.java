// consulo: package org.jetbrains.bazel.languages.starlark.completion is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.languages.starlark.completion.java;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.PsiLiteralExpression;
import com.intellij.java.language.psi.PsiModifier;
import com.intellij.java.language.psi.PsiModifierList;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.reference.StarlarkClassnameReferenceProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionLookupElement.getCompletionLookupElement;

@ExtensionImpl
public class StarlarkClassnameCompletionContributor extends CompletionContributor {
    public StarlarkClassnameCompletionContributor() {
        extend(CompletionType.BASIC, StarlarkClassnameCompletionProvider.psiPattern, new StarlarkClassnameCompletionProvider());
    }

    // consulo: completion contributors are language-bound extensions (upstream XML language="Starlark")
    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}

class StarlarkClassnameCompletionProvider implements CompletionProvider {
    static final PsiElementPattern.Capture<PsiElement> psiPattern =
        psiElement()
            .withLanguage(StarlarkLanguage.INSTANCE)
            .inside(
                psiElement(StarlarkStringLiteralExpression.class)
                    .with(
                        new PatternCondition<StarlarkStringLiteralExpression>("isClassnameValue") {
                            @Override
                            public boolean accepts(StarlarkStringLiteralExpression element, ProcessingContext context) {
                                return StarlarkClassnameReferenceProvider.isClassnameValue(element);
                            }
                        }
                    )
            );


    // KotlinClassHeader.Kind.SYNTHETIC_CLASS.id
    private static final int KOTLIN_SYNTHETIC_CLASS = 3;

    // KotlinClassHeader.Kind.MULTIFILE_CLASS_PART.id
    private static final int KOTLIN_MULTIFILE_CLASS_PART = 5;

    @Override
    @RequiredReadAction
    public void addCompletions(
        CompletionParameters parameters,
        ProcessingContext context,
        CompletionResultSet resultSet
    ) {
        if (!(parameters.getPosition().getParent() instanceof StarlarkStringLiteralExpression element)) {
            return;
        }
        if (!org.jetbrains.bazel.config.project.isBazelProject(element.getProject())) {
            return;
        }

        String fullText = element.getStringContents();
        List<String> fragments = Arrays.asList(fullText.split("\\.", -1));
        String prefix = String.join(".", fragments.subList(0, Math.max(fragments.size() - 1, 0)));

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(element.getProject());
        GlobalSearchScope scope = GlobalSearchScope.allScope(element.getProject());

        // Handle packages and their top-level classes
        PsiJavaPackage packageToSearch = psiFacade.findPackage(prefix);
        if (packageToSearch != null) {
            resultSet.addAllElements(getClassesVariants(packageToSearch.getClasses()));
            resultSet.addAllElements(getSubPackagesVariants(packageToSearch.getSubPackages()));
        }

        // Handle nested classes
        PsiClass classToSearch = psiFacade.findClass(prefix, scope);
        if (classToSearch != null) {
            resultSet.addAllElements(getClassesVariants(classToSearch.getInnerClasses()));
        }
    }

    // consulo: PsiPackage -> PsiJavaPackage
    private List<LookupElement> getSubPackagesVariants(PsiJavaPackage[] subpackages) {
        return Arrays.stream(subpackages).map(subPackage ->
            getCompletionLookupElement(
                /* name = */ subPackage.getQualifiedName() + ".",
                /* icon = */ PlatformIconGroup.nodesPackage(),
                /* priority = */ 1.0,
                /* presentableText = */ subPackage.getName() + ".",
                /* tailText = */ " (" + subPackage.getQualifiedName() + ")",
                /* isExpressionFinished = */ false
            )
        ).toList();
    }

    @RequiredReadAction
    private List<LookupElement> getClassesVariants(PsiClass[] classes) {
        return Arrays.stream(classes)
            .filter(it -> isCompletableClass(it))
            .flatMap(psiClass -> {
                List<LookupElement> result = new ArrayList<>();
                String qualifiedName = psiClass.getQualifiedName();
                if (qualifiedName != null) {
                    String name = qualifiedName.replace('$', '.');

                    // Add the class itself
                    result.add(
                        getCompletionLookupElement(
                            /* name = */ name,
                            /* icon = */ PlatformIconGroup.nodesClass(),
                            /* priority = */ 1.0,
                            /* presentableText = */ psiClass.getName(),
                            /* tailText = */ " (" + name + ")"
                        )
                    );

                    // Add the class as a path segment if it has completable inner classes
                    if (Arrays.stream(psiClass.getInnerClasses()).anyMatch(it -> isCompletableClass(it))) {
                        result.add(
                            getCompletionLookupElement(
                                /* name = */ name + ".",
                                /* icon = */ PlatformIconGroup.nodesClass(),
                                /* priority = */ 1.0,
                                /* presentableText = */ psiClass.getName() + ".",
                                /* tailText = */ " (" + name + ")",
                                /* isExpressionFinished = */ false
                            )
                        );
                    }
                }
                return result.stream();
            })
            .toList();
    }

    @RequiredReadAction
    private boolean isCompletableClass(PsiClass receiver) {
        // Filter out interfaces and abstract classes
        if (receiver.isInterface() || receiver.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return false;
        }

        // Exclude anonymous/local classes
        String qName = receiver.getQualifiedName();
        if (qName == null) {
            return false;
        }
        String name = receiver.getName();
        if (name == null) {
            return false;
        }

        @Nullable Integer metadataKind = null;
        PsiModifierList modifierList = receiver.getModifierList();
        PsiAnnotation ann = modifierList != null ? modifierList.findAnnotation("kotlin.Metadata") : null;
        if (ann != null
            && ann.findAttributeValue("k") instanceof PsiLiteralExpression literal
            && literal.getValue() instanceof Integer value) {
            metadataKind = value;
        }

        // Exclude synthetic classes
        if (metadataKind != null && metadataKind == KOTLIN_SYNTHETIC_CLASS) {
            return false;
        }

        // Exclude kotlin multi-file part implementations (e.g. FooKt__partN)
        if (metadataKind != null && metadataKind == KOTLIN_MULTIFILE_CLASS_PART) {
            return false;
        }
        if (metadataKind == null && name.contains("Kt__")) {
            return false;
        }

        // Exclude Kotlin helper classes for default interface methods
        if (name.endsWith("$DefaultImpls")) {
            return false;
        }

        // Exclude compiler/JVM artifacts e.g. lambdas
        if (qName.contains("$$")) {
            return false;
        }

        return true;
    }
}
