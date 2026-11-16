package org.jetbrains.bazel.java.ui.gutters;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiClassOwner;
import com.intellij.java.language.psi.util.ClassUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.sync.workspace.languages.jvm.DerivedEntryPointClass;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaBazelRules;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.gutters.BazelEntryPointMatcher;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Matches the test class of a JVM test, or the main class of a JVM binary, with the classes that hold a PSI element.
 * The matcher compares the class that Bazel runs. It takes a derived class from the path, as Bazel does, and not from the PSI of the file.
 */
@ExtensionImpl
public class JvmEntryPointMatcher implements BazelEntryPointMatcher {
    public JvmEntryPointMatcher() {
    }

    @Override
    public Map<Label, ThreeState> match(PsiElement element, List<BuildTargetInfo> targets) {
        PsiClassOwner file = element.getContainingFile() instanceof PsiClassOwner classOwner ? classOwner : null;
        if (file == null || DumbService.isDumb(file.getProject())) {
            Map<Label, ThreeState> result = new LinkedHashMap<>();
            for (BuildTargetInfo it : targets) {
                result.put(it.id(), ThreeState.UNSURE);
            }
            return result;
        }
        Supplier<Set<String>> classNames = LazyValue.notNull(() -> collectClassNames(file.getClasses(), element));
        Map<Label, ThreeState> result = new LinkedHashMap<>();
        for (BuildTargetInfo target : targets) {
            String entryPoint = entryPointClass(file.getProject(), target);
            if (entryPoint == null) {
                result.put(target.id(), ThreeState.UNSURE);
                continue;
            }
            result.put(target.id(), classNames.get().contains(entryPoint) ? ThreeState.YES : ThreeState.NO);
        }
        return result;
    }

    /**
     * Returns the binary names of the {@code classes} and their inner classes that hold {@code element} or that are in {@code element}.
     * Bazel gives `test_class` and `main_class` to `Class.forName`, so they are binary names, such as {@code com.foo.Outer$Inner}.
     * <p>
     * A Kotlin file facade class counts only when no declared class holds {@code element}, as for a top-level function.
     * <p>
     * It handles Java and Kotlin. For a Kotlin file, {@link PsiClassOwner#getClasses} returns light classes, which are not in the PSI
     * tree of the file. {@link PsiClass#getNavigationElement} gives the actual Kotlin PSI, so the matcher can compare it with
     * {@code element}.
     */
    private Set<String> collectClassNames(PsiClass[] classes, PsiElement element) {
        List<PsiClass> facadeClasses = new ArrayList<>();
        List<PsiClass> declaredClasses = new ArrayList<>();
        Stream.of(classes)
            .flatMap(JvmEntryPointMatcher::withInnerClasses)
            .forEach(it -> (it.getNavigationElement() instanceof PsiFile ? facadeClasses : declaredClasses).add(it));
        List<PsiClass> relatedClasses = declaredClasses.stream()
            .filter(it -> PsiTreeUtil.isAncestor(element, it.getNavigationElement(), false)
                || PsiTreeUtil.isAncestor(it.getNavigationElement(), element, false))
            .toList();
        boolean isInDeclaredClass = relatedClasses.stream().anyMatch(it -> PsiTreeUtil.isAncestor(it.getNavigationElement(), element, false));
        List<PsiClass> matchingClasses = new ArrayList<>(relatedClasses);
        if (!isInDeclaredClass) {
            matchingClasses.addAll(facadeClasses);
        }
        Set<String> result = new HashSet<>();
        for (PsiClass it : matchingClasses) {
            // consulo: IntelliJ ClassUtil.getBinaryClassName -> ClassUtil.getJVMClassName (same result for top-level and inner classes)
            String name = ClassUtil.getJVMClassName(it);
            if (name != null) {
                result.add(name);
            }
        }
        return result;
    }

    private static Stream<PsiClass> withInnerClasses(PsiClass receiver) {
        return Stream.concat(Stream.of(receiver), Stream.of(receiver.getInnerClasses()).flatMap(JvmEntryPointMatcher::withInnerClasses));
    }

    private @Nullable String entryPointClass(Project project, BuildTargetInfo target) {
        JvmBuildTarget jvmTarget = TargetStorage.getTargetStorage(project).getTargetDataForLabel(target.id(), JvmBuildTarget.class);
        if (jvmTarget == null) {
            return null;
        }
        RuleType ruleType = target.kind().ruleType();
        if (ruleType == RuleType.TEST) {
            String testClass = jvmTarget.testClass();
            return testClass != null ? testClass : derivedClass(project, target, jvmTarget);
        }
        else if (ruleType == RuleType.BINARY) {
            String mainClass = jvmTarget.mainClass();
            return mainClass != null ? mainClass : derivedClass(project, target, jvmTarget);
        }
        else {
            return null;
        }
    }

    private @Nullable String derivedClass(Project project, BuildTargetInfo target, JvmBuildTarget jvmTarget) {
        String kind = target.kind().kind();
        // For a `java_test` with a custom runner in `main_class` and no `test_class`, the matcher does not guess the test class.
        // `rules_kotlin` sets a default `main_class` on each `kt_jvm_test`, so the check is only for `java_test`.
        if (kind.equals(JavaBazelRules.JAVA_TEST_KIND.kind()) && jvmTarget.mainClass() == null) {
            BuildTarget loaded = loadTarget(project, target);
            return loaded == null ? null : DerivedEntryPointClass.rulesJavaPrimaryClass(loaded);
        }
        else if (kind.equals(JavaBazelRules.JAVA_BINARY_KIND.kind())) {
            BuildTarget loaded = loadTarget(project, target);
            return loaded == null ? null : DerivedEntryPointClass.rulesJavaPrimaryClass(loaded);
        }
        else if (kind.equals(JavaBazelRules.KT_JVM_TEST_KIND.kind())) {
            BuildTarget loaded = loadTarget(project, target);
            return loaded == null ? null : DerivedEntryPointClass.rulesKotlinTestClass(loaded);
        }
        else {
            return null;
        }
    }

    private @Nullable BuildTarget loadTarget(Project project, BuildTargetInfo target) {
        return TargetStorage.getBuildTarget(TargetStorage.getTargetStorage(project), target.id());
    }
}
