package org.jetbrains.bazel.golang.run;

import com.goide.runconfig.testing.GoTestLocator;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.sm.runner.SMTestLocator;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.golang.targetKinds.GoBazelRules;
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties.BazelTestLocatorProvider;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BazelTestLocationHintProvider;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class BazelGoTestLocationHintProvider implements BazelTestLocationHintProvider {
    @Override
    public boolean isApplicable(TargetKind targetKind) {
        return GoBazelRules.includesGo(targetKind);
    }

    @Override
    public @Nullable String getLocationHint(BazelTestDetails testDetails) {
        String suiteName;
        if (testDetails.isSuite()) {
            suiteName = testDetails.displayName();
        }
        else {
            if (testDetails.parentSuiteNames().isEmpty()) {
                return null;
            }
            suiteName = testDetails.parentSuiteNames().get(0);
        }
        int importPathDotSeparator = suiteName.lastIndexOf('.');
        if (importPathDotSeparator == -1) {
            return null;
        }
        String importPath = suiteName.substring(0, importPathDotSeparator);
        String outerTestName = suiteName.substring(importPathDotSeparator + 1);
        String subtestName = !testDetails.isSuite() ? testDetails.displayName() : null;
        String testName;
        if (outerTestName.equals(subtestName) || subtestName == null) {
            testName = outerTestName;
        }
        else {
            testName = outerTestName + "/" + subtestName;
        }
        // consulo: GoSharedConstants.TEST_PROTOCOL -> GoTestLocator.PROTOCOL ("gotest")
        // TODO consulo: the GoTestLocator of consulo-google-go resolves "gotest://<function>" or "gotest://<type>.<method>",
        //  not the "gotest://<import path>#<test>" hint of IntelliJ
        return GoTestLocator.PROTOCOL + "://" + importPath + "#" + testName;
    }
}

@ExtensionImpl
class BazelGoTestLocatorProvider implements BazelTestLocatorProvider {
    // consulo: upstream GoTestLocator(null), the Consulo locator is a singleton
    private final SMTestLocator myLocator = GoTestLocator.INSTANCE;

    public BazelGoTestLocatorProvider() {
    }

    @Override
    public SMTestLocator getTestLocator() {
        return myLocator;
    }
}
