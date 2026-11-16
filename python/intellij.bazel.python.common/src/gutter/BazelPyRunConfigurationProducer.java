package org.jetbrains.bazel.python.gutter;

import com.jetbrains.python.psi.PyFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.python.run.PythonBazelRunUtils;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@ExtensionImpl
public class BazelPyRunConfigurationProducer extends BazelRunConfigurationProducer {
    @Override
    @RequiredReadAction
    public @Nullable GutterAction getGutterAction(PsiElement element, BuildTargetInfo target) {
        if (!(element.getContainingFile() instanceof PyFile) || !PythonBazelRunUtils.isRunLineMarkerElement(element)) {
            return null;
        }
        return new GutterAction(
            null,
            PythonBazelRunUtils.getTestRunnerArguments(element),
            Map.of(),
            null
        );
    }
}
