package org.jetbrains.bazel.inspections;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.jvm.JvmClass;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiJavaFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.java.localize.JavaInspectionsLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.workspacemodel.entities.extensions;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelModuleExtensionEntity;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

// consulo: registration of the upstream <localInspection> (language, shortName, groupKey, key, enabledByDefault, level)
@ExtensionImpl
public class JavaStrictDependenciesInspection extends LocalInspectionTool {

    @Override
    public PsiElementVisitor buildVisitor(
        ProblemsHolder holder,
        boolean isOnTheFly
    ) {
        if (!(holder.getFile() instanceof PsiJavaFile psiFile)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        BazelModuleExtensionEntity bazelModuleExtension = bazelModuleExtension(psiFile);
        if (bazelModuleExtension == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        Label moduleLabel = bazelModuleExtension.getTargetKey().label();
        StrictDependencyCheckedType strictDependenciesCheck = bazelModuleExtension.strictDependencies().check();
        Set<String> strictDependencies = new HashSet<>(bazelModuleExtension.strictDependencies().labels());
        if (strictDependenciesCheck == StrictDependencyCheckedType.OFF || strictDependencies.isEmpty()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        Predicate<Label> isDirectDependency = label ->
            moduleLabel.equals(label) || strictDependencies.contains(label.toString());

        Project project = psiFile.getProject();

        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
                PsiReference it = reference.getReference();
                if (it != null) {
                    checkReference(it);
                }
            }

            @RequiredReadAction
            public void checkReference(PsiReference reference) {
                if (!(reference.resolve() instanceof JvmClass psiClass)) {
                    return;
                }

                // consulo: multiverse (code insight contexts) is not ported, so no explicit context is passed
                List<Label> psiClassLabels = getTargetLabels(psiClass.getSourceElement()).toList();
                if (psiClassLabels.size() != 1) {
                    return;
                }
                Label psiClassLabel = psiClassLabels.get(0);
                if (isDirectDependency.test(psiClassLabel)) {
                    return;
                }

                // Several targets may provide the very same class: two maven installs shipping the same
                // artifact, or one source file listed in the srcs of two targets. Resolution picks just one
                // of them, and that one may be an indirect dependency even though a direct dependency
                // provides the same class, in which case Bazel itself is happy. Report only when no target
                // providing this class is a direct dependency.
                if (allTargetLabelsOf(psiClass, reference.getElement()).anyMatch(isDirectDependency)) {
                    return;
                }

                Label apparentLabel = BazelRepoMappingUtils.toApparentLabelOrThis(psiClassLabel, project);
                String qualifiedName = psiClass.getQualifiedName();
                String name = psiClass.getName();
                holder.newProblem(
                        JavaInspectionsLocalize.javaStrictDepsIndirectDependency(
                            qualifiedName != null ? qualifiedName : name != null ? name : "<undefined>",
                            apparentLabel
                        )
                    )
                    .range(reference.getElement())
                    .highlightType(
                        strictDependenciesCheck == StrictDependencyCheckedType.ERROR
                            ? ProblemHighlightType.ERROR
                            : ProblemHighlightType.WARNING
                    )
                    .create();
            }

            // consulo: upstream getTargetLabels(element, explicitContext: CodeInsightContext?) first maps the code insight
            //   contexts (multiverse) of the file to labels; multiverse is not ported, so only the fallback remains
            @RequiredReadAction
            private Stream<Label> getTargetLabels(@Nullable PsiElement element) {
                PsiFile file = element != null ? element.getContainingFile() : null;
                if (file == null) {
                    return Stream.empty();
                }
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile == null) {
                    return Stream.empty();
                }

                // Fallback
                Module module = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile);
                BazelModuleExtensionEntity moduleExtension = module != null ? extensions.getBazelModuleExtension(module) : null;
                Label fallbackLabel = moduleExtension != null ? moduleExtension.getTargetKey().label() : null;
                return fallbackLabel != null ? Stream.of(fallbackLabel) : Stream.empty();
            }

            // consulo: upstream getTargetLabel(context: CodeInsightContext, project) maps a multiverse ModuleContext /
            //   LibraryContext to the Bazel module / library extension label; multiverse is not ported

            /**
             * Labels of every target that provides {@code psiClass} to {@code place}: all the classes with the same qualified
             * name visible from {@code place}, each of them mapped through all of the contexts of its containing file,
             * because a single source file may belong to several targets.
             */
            @RequiredReadAction
            private Stream<Label> allTargetLabelsOf(JvmClass psiClass, PsiElement place) {
                String qualifiedName = psiClass.getQualifiedName();
                if (qualifiedName == null) {
                    return Stream.empty();
                }
                PsiClass[] classes = JavaPsiFacade.getInstance(project)
                    .findClasses(qualifiedName, place.getResolveScope());
                return Arrays.stream(classes)
                    .flatMap(it -> getTargetLabels(it.getContainingFile()));
            }
        };
    }

    @RequiredReadAction
    private @Nullable BazelModuleExtensionEntity bazelModuleExtension(@Nullable PsiFile file) {
        if (file == null) {
            return null;
        }

        // consulo: multiverse (ModuleContext of file.codeInsightContext) is not ported

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        Module module = ProjectFileIndex.getInstance(file.getProject()).getModuleForFile(virtualFile);
        return module != null ? extensions.getBazelModuleExtension(module) : null;
    }

    @Override
    public @Nullable Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "JavaStrictDependencies";
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return JavaInspectionsLocalize.bazelInspectionGroupName();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return JavaInspectionsLocalize.inspectionsDisplayNameJavastrictdeps();
    }

    // consulo: the description is a LocalizeValue (upstream inspectionDescriptions/JavaStrictDependencies.html); a resource directory
    //   shared with consulo.bazel.base would be a split package
    @Override
    public LocalizeValue getDescription() {
        return JavaInspectionsLocalize.inspectionJavaStrictDependenciesDescription();
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
