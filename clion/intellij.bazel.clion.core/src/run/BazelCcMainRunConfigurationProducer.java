package org.jetbrains.bazel.clion.run;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.clion.sync.CcLanguageClass;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream runConfigurationProducer), CppFileEntryPointDetector (com.jetbrains.cidr.cpp.runfile) does not
//   exist in consulo-cpp, which cannot tell whether an element is the main function or inside it
public class BazelCcMainRunConfigurationProducer extends BazelRunConfigurationProducer {
    @Override
    public @Nullable GutterAction getGutterAction(PsiElement element, BuildTargetInfo target) {
        if (!CcLanguageClass.includesCc(target.kind())) {
            return null;
        }
        // TODO consulo: CppFileEntryPointDetector does not exist in consulo-cpp, so no element is detected as the main function
        // if (CppFileEntryPointDetector.getInstance()?.isMainOrIsInMain(element) != true) {
        //   return null
        // }
        // return GutterAction()
        return null;
    }
}
