// consulo: package org.jetbrains.bazel.testing is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.testing.java;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.java.JavaLanguageClass;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BazelTestLocationHintProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@ExtensionImpl
public class BazelJavaTestLocationHintProvider implements BazelTestLocationHintProvider {
    @Override
    public boolean isApplicable(TargetKind targetKind) {
        return JavaLanguageClass.includesJava(targetKind);
    }

    @Override
    public String getLocationHint(BazelTestDetails testDetails) {
        if (testDetails.isSuite()) {
            return testSuiteLocationHint(testDetails.displayName(), testDetails.classname(), testDetails.parentSuiteNames());
        }
        else {
            return testCaseLocationHint(testDetails.displayName(), testDetails.classname(), testDetails.parentSuiteNames());
        }
    }

    // consulo: overload for the upstream default arguments classname = null, parentSuites = emptyList()
    public String testCaseLocationHint(String testName) {
        return testCaseLocationHint(testName, null, List.of());
    }

    /**
     * Generates a location hint for a test case.
     * If non-null `classname` is given, `parentSuites` is ignored - it serves as a fallback if `classname` is unknown
     */
    public String testCaseLocationHint(
        String testName,
        @Nullable String classname,
        List<String> parentSuites
    ) {
        String classnameToUse = classname != null ? classname : String.join(SUITE_DELIMITER, parentSuites);
        String cleanClassName = removeMisleadingDelimiters(classnameToUse);
        String cleanTestName = testName.endsWith("()") ? testName.substring(0, testName.length() - 2) : testName;
        return TEST_CASE_PREFIX + cleanClassName + FRAGMENT_DELIMITER + cleanTestName;
    }

    // consulo: overload for the upstream default arguments classname = null, parentSuites = emptyList()
    public String testSuiteLocationHint(String suiteName) {
        return testSuiteLocationHint(suiteName, null, List.of());
    }

    /**
     * Generates a location hint for a test suite.
     * If non-null `classname` is given, `parentSuites` is ignored - it serves as a fallback if `classname` is unknown
     */
    public String testSuiteLocationHint(
        String suiteName,
        @Nullable String classname,
        List<String> parentSuites
    ) {
        String classnameToUse;
        if (classname != null) {
            classnameToUse = classname;
        }
        else if (parentSuites.isEmpty()) {
            classnameToUse = suiteName;
        }
        else {
            String generatedClassname = String.join(SUITE_DELIMITER, parentSuites) + SUITE_DELIMITER + suiteName;
            classnameToUse = generatedClassname;
        }
        String cleanClassName = removeMisleadingDelimiters(classnameToUse);
        return TEST_SUITE_PREFIX + cleanClassName;
    }

    private static String removeMisleadingDelimiters(String receiver) {
        return receiver.replace(FRAGMENT_DELIMITER, SUITE_DELIMITER); // otherwise the delimiters might get misinterpreted
    }

    public static BazelJavaTestLocationHintProvider getInstance() {
        return Objects.requireNonNull(BazelTestLocationHintProvider.getEp().findExtension(BazelJavaTestLocationHintProvider.class));
    }

    // consulo: upstream top-level constants of the file (TEST_SUITE_PREFIX, TEST_CASE_PREFIX are internal, used by
    //   DefaultJvmTestRerunExtension)
    public static final String TEST_SUITE_PREFIX = "java:suite://";
    public static final String TEST_CASE_PREFIX = "java:test://";
    private static final String SUITE_DELIMITER = "$";
    private static final String FRAGMENT_DELIMITER = "/";
}
