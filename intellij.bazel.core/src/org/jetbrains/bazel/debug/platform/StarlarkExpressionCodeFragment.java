package org.jetbrains.bazel.debug.platform;

import consulo.language.file.FileViewProvider;
import consulo.language.file.LanguageFileViewProviderFactory;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jspecify.annotations.Nullable;

public class StarlarkExpressionCodeFragment extends StarlarkFile {
    private final @Nullable PsiElement myContext;

    public StarlarkExpressionCodeFragment(
        Project project,
        String extensionlessName,
        String content,
        @Nullable PsiElement context
    ) {
        super(
            createFileViewProvider(
                project,
                new LightVirtualFile(withExtension(extensionlessName), StarlarkFileType.INSTANCE, content)
            )
        );
        myContext = context;
    }

    // consulo: upstream PsiManagerEx.getInstanceEx(project).fileManager.createFileViewProvider(file, true) is internal API;
    //   the Starlark view provider factory is asked directly
    private static FileViewProvider createFileViewProvider(Project project, LightVirtualFile file) {
        PsiManager manager = PsiManager.getInstance(project);
        LanguageFileViewProviderFactory factory = LanguageFileViewProviderFactory.forLanguage(StarlarkLanguage.INSTANCE);
        if (factory != null) {
            return factory.createFileViewProvider(file, StarlarkLanguage.INSTANCE, manager, true);
        }
        return new SingleRootFileViewProvider(manager, file, true);
    }

    @Override
    public @Nullable PsiElement getContext() {
        PsiElement context = myContext;
        return context != null && context.isValid() ? context : super.getContext();
    }

    private static String withExtension(String receiver) {
        return receiver + "." + StarlarkFileType.INSTANCE.getDefaultExtension();
    }
}
