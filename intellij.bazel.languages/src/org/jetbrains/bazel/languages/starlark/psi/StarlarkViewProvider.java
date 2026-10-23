package org.jetbrains.bazel.languages.starlark.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.file.LanguageFileViewProviderFactory;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;

class StarlarkViewProvider extends SingleRootFileViewProvider {
    StarlarkViewProvider(PsiManager manager, VirtualFile virtualFile, boolean eventSystemEnabled, Language language) {
        super(manager, virtualFile, eventSystemEnabled, language);
    }

    // Make sure to create Starlark PSI even for excluded directories (such as bazel-out)
    @Override
    protected boolean shouldCreatePsi() {
        return getBaseLanguage() instanceof StarlarkLanguage;
    }

    @Override
    public SingleRootFileViewProvider createCopy(VirtualFile copy) {
        return new StarlarkViewProvider(getManager(), copy, false, getBaseLanguage());
    }
}

@ExtensionImpl
class StarlarkViewProviderFactory implements LanguageFileViewProviderFactory {
    // consulo: extensions need a public constructor
    public StarlarkViewProviderFactory() {
    }

    @Override
    public FileViewProvider createFileViewProvider(
        VirtualFile file,
        Language language,
        PsiManager manager,
        boolean eventSystemEnabled
    ) {
        return new StarlarkViewProvider(manager, file, eventSystemEnabled, language);
    }

    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
