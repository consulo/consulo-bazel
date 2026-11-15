package org.jetbrains.bazel.clion.run;

import consulo.language.psi.PsiElement;
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jspecify.annotations.Nullable;

// TODO consulo: not registered (upstream runConfigurationProducer), BazelCcTestFrameworkSupport.findTestAndFramework and
//   createTestFilter need CidrTestScopeElement (com.jetbrains.cidr.execution.testing), which does not exist in consulo-cpp
public class BazelCcTestRunConfigurationProducer extends BazelRunConfigurationProducer {
    @Override
    public @Nullable GutterAction getGutterAction(
        PsiElement element,
        BuildTarget target
    ) {
        // TODO consulo: CidrTestScopeElement does not exist in consulo-cpp
        // val (testElement, framework) = BazelCcTestFrameworkSupport.findTestAndFramework(element) ?: return null
        // val testFilter = framework.createTestFilter(listOf(testElement)) ?: return null
        // return GutterAction(testFilter = testFilter, additionalLocationString = testElement.testName)
        return null;
    }
}
