package org.jetbrains.bazel.languages.projectview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiElement;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

// FIXME: this interface is used to decouple project view section definitions from targetUtils
//  in the ideal case each section could define reference/key to `CompletionProvider` and then backend
//  can link this reference to the actual completion provider
//  new API would contain data access endpoints for code insight in separate artifact
//
// TODO: I'm going to redesign code insights API for entire plugin in order to provide
//  unified way of interacting with data obtained from bazel and this will allow us to fully
//  decouple IJ code analysis stuff from target utils
@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectViewCodeInsightSupport {
    Stream<String> getAllTargetLabels();

    Set<VirtualFile> getExcludedRoots();

    @Nullable PsiElement resolvePsiFromLabel(Label label, @Nullable VirtualFile containingFile, boolean acceptOnlyFileTarget);

    // consulo: overloads for the upstream default arguments
    default @Nullable PsiElement resolvePsiFromLabel(Label label) {
        return resolvePsiFromLabel(label, null, false);
    }

    default @Nullable PsiElement resolvePsiFromLabel(Label label, @Nullable VirtualFile containingFile) {
        return resolvePsiFromLabel(label, containingFile, false);
    }
}
