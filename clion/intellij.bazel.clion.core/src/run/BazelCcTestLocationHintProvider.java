package org.jetbrains.bazel.clion.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.io.URLUtil;
import org.jetbrains.bazel.clion.sync.CcLanguageClass;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BazelTestLocationHintProvider;

/**
 * We don't know beforehand which testing framework is used, so we use a generic protocol instead
 */
@ExtensionImpl
public class BazelCcTestLocationHintProvider implements BazelTestLocationHintProvider {
    // consulo: upstream Kotlin top-level constants of BazelCcTestLocationHintProvider.kt
    public static final String BAZEL_CC_PROTOCOL = "bazel:test:cc";

    public static final String TEST_NAME_PARTS_SPLITTER = "::";

    @Override
    public boolean isApplicable(TargetKind targetKind) {
        return CcLanguageClass.includesCc(targetKind);
    }

    @Override
    public String getLocationHint(BazelTestDetails testDetails) {
        String firstSuite = testDetails.parentSuiteNames().isEmpty() ? "" : testDetails.parentSuiteNames().get(0);
        return BAZEL_CC_PROTOCOL + URLUtil.SCHEME_SEPARATOR + firstSuite + TEST_NAME_PARTS_SPLITTER + testDetails.displayName();
    }
}
