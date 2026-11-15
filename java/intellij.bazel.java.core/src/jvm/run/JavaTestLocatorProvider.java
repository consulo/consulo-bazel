package org.jetbrains.bazel.jvm.run;

import com.intellij.java.execution.impl.testframework.JavaTestLocator;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.sm.runner.SMTestLocator;
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties;

@ExtensionImpl(id = "defaultJavaLocator")
public class JavaTestLocatorProvider implements BazelTestConsoleProperties.BazelTestLocatorProvider {
    @Override
    public SMTestLocator getTestLocator() {
        return JavaTestLocator.INSTANCE;
    }
}
