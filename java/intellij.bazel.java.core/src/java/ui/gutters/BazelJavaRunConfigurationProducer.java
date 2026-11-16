package org.jetbrains.bazel.java.ui.gutters;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.util.ClassUtil;
import com.intellij.java.language.psi.util.PsiMethodUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

// External plugins (e.g., Scala) should extend BazelRunConfigurationProducer instead
// consulo: upstream isDumbAware() = true; BazelRunConfigurationProducer already implements DumbAware
@ExtensionImpl
public class BazelJavaRunConfigurationProducer extends BazelRunConfigurationProducer {
    @Override
    @RequiredReadAction
    public @Nullable GutterAction getGutterAction(PsiElement element, BuildTargetInfo target) {
        // consulo: JarFileSystem -> ArchiveFileSystem
        PsiFile containingFile = element.getContainingFile();
        VirtualFile virtualFile = containingFile != null ? containingFile.getVirtualFile() : null;
        if (virtualFile != null && virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
            return null;
        }
        PsiNameIdentifierOwner psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner.class, true);
        if (psiIdentifier == null) {
            return null;
        }
        if (psiIdentifier.getNameIdentifier() != element) {
            return null;
        }
        Pair<@Nullable PsiClass, @Nullable PsiMethod> classOrMethodPair = toPsiClassOrMethod(psiIdentifier);
        PsiClass psiClass = classOrMethodPair.getFirst();
        PsiMethod psiMethod = classOrMethodPair.getSecond();
        PsiElement classOrMethod = psiClass != null ? psiClass : psiMethod;
        if (classOrMethod == null) {
            return null;
        }
        if (isMainMethod(element)) {
            return new GutterAction();
        }

        String className = getContainingClassFqn(psiIdentifier);
        if (className == null) {
            return null;
        }
        String testFilter = JvmTestFilterExtension.getInstance(element.getProject(), target).getTestFilter(className, psiMethod);
        // TODO consulo: com.intellij.execution.junit.DisabledConditionUtil is missing in consulo-junit
        //val junitDisabledCondition = DisabledConditionUtil.getDisabledCondition(classOrMethod)
        @Nullable String junitDisabledCondition = null;
        List<String> programArguments;
        if (junitDisabledCondition != null) {
            // Support running a @Disabled JUnit test if we clicked on it explicitely
            programArguments = List.of("--wrapper_script_flag=--jvm_flag=-Djunit.jupiter.conditions.deactivate=" + junitDisabledCondition);
        }
        else {
            programArguments = List.of();
        }
        return new GutterAction(
            /* testFilter = */ testFilter,
            /* programArguments = */ programArguments,
            /* env = */ Map.of(),
            /* additionalLocationString = */ psiMethod != null ? psiMethod.getName() : null
        );
    }

    public @Nullable String getContainingClassFqn(PsiElement element) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        if (psiClass == null) {
            return null;
        }
        // consulo: JvmClassUtil.getJvmClassName -> ClassUtil.getJVMClassName
        return ClassUtil.getJVMClassName(psiClass);
    }

    public Pair<@Nullable PsiClass, @Nullable PsiMethod> toPsiClassOrMethod(PsiNameIdentifierOwner element) {
        return Pair.create(
            element instanceof PsiClass psiClass ? psiClass : null,
            element instanceof PsiMethod psiMethod ? psiMethod : null
        );
    }

    public boolean isMainMethod(PsiElement element) {
        PsiNameIdentifierOwner identifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner.class, true);
        // consulo: JvmMainMethodUtil.isMainMethod / hasMainMethodInHierarchy -> PsiMethodUtil.isMainMethod / hasMainMethod
        //   (hasMainMethod searches the methods of the class hierarchy)
        return identifier instanceof PsiMethod psiMethod && PsiMethodUtil.isMainMethod(psiMethod)
            || identifier instanceof PsiClass psiClass && PsiMethodUtil.hasMainMethod(psiClass);
    }

    // consulo: upstream top-level declarations of BazelJavaRunConfigurationProducer.kt

    public static String getTestFilter(String className, @Nullable String methodName) {
        if (methodName != null) {
            // Include
            return normalizeNestedClassSeparator(className) + "." + methodName + "$";
        }
        else {
            return normalizeNestedClassSeparator(className);
        }
    }

    /**
     * Any `$` separating a nested class is replaced with `.`, because `$` would otherwise be interpreted as a regex end-of-input anchor.
     */
    private static String normalizeNestedClassSeparator(String receiver) {
        return receiver.replace("$", ".");
    }
}
