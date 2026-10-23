package org.jetbrains.bazel.languages.starlark;

import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType;

import java.util.Set;

public final class StarlarkSearchScope {
    private StarlarkSearchScope() {
    }

    public static GlobalSearchScope starlarkProjectScope(Project receiver) {
        return restrictToStarlarkFiles(GlobalSearchScope.projectScope(receiver));
    }

    public static GlobalSearchScope restrictToStarlarkFiles(GlobalSearchScope receiver) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(
            receiver,
            StarlarkFileType.INSTANCE
        );
    }

    public static GlobalSearchScope restrictToStarlarkFiles(
        GlobalSearchScope receiver,
        BazelFileType... types
    ) {
        return new StarlarkFilesByTypeScope(restrictToStarlarkFiles(receiver), Set.of(types));
    }
}

class StarlarkFilesByTypeScope extends DelegatingGlobalSearchScope {
    private final Set<BazelFileType> myTypes;

    StarlarkFilesByTypeScope(
        GlobalSearchScope base,
        Set<BazelFileType> types
    ) {
        super(base);
        myTypes = types;
    }

    @Override
    public boolean contains(VirtualFile file) {
        return super.contains(file) && myTypes.contains(BazelFileType.ofFileName(file.getName()));
    }
}
