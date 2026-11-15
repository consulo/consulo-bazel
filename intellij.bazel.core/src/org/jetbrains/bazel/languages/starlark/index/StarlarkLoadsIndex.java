package org.jetbrains.bazel.languages.starlark.index;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.function.Processor;
import consulo.index.io.ID;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public final class StarlarkLoadsIndex {
    private StarlarkLoadsIndex() {
    }

    public static final ID<String, Collection<String>> NAME = ID.create("starlark.loads");

    /**
     * Process files that load {@code element} from given {@code scope}.
     */
    @RequiredReadAction
    public static boolean processFiles(
        StarlarkNamedElement element,
        GlobalSearchScope scope,
        Processor<VirtualFile> processor
    ) {
        String name = element.getName();
        if (name == null) {
            return false;
        }
        PsiFile elementFile = element.getContainingFile();
        VirtualFile containingFile = elementFile == null ? null : elementFile.getVirtualFile();
        if (containingFile == null) {
            return false;
        }
        Project project = element.getProject();
        return FileBasedIndex
            .getInstance()
            .processValues(
                NAME,
                name,
                null,
                (file, labels) ->
                    file.equals(containingFile)
                        || labels.stream()
                        .noneMatch(it -> Objects.equals(resolveFileTargetToVirtualFile(project, it, file), containingFile))
                        || processor.process(file),
                scope
            );
    }

    /**
     * Checks whether {@code element} was loaded in {@code scope}.
     */
    @RequiredReadAction
    public static boolean isLoaded(StarlarkNamedElement element, GlobalSearchScope scope) {
        boolean[] found = {false};
        processFiles(element, scope, it -> {
            found[0] = true;
            return false;
        });
        return found[0];
    }

    private static @Nullable VirtualFile resolveFileTargetToVirtualFile(Project project, String rawLabel, VirtualFile file) {
        Label label = Label.parseOrNull(rawLabel);
        if (label == null) {
            return null;
        }
        return LabelResolveUtils.resolveFileTargetToVirtualFile(project, label, file);
    }
}
