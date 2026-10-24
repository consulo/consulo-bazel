package org.jetbrains.bazel.server.bep;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BspClientTestNotifier;
import org.jetbrains.bsp.protocol.TaskFinishParams.JUnitStyleTestCaseData;
import org.jetbrains.bsp.protocol.TaskFinishParams.JUnitStyleTestSuiteData;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TestStatus;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// consulo: the compact constructors replace the Kotlin module of Jackson: they apply the Kotlin default values
// (Jackson passes null for a missing element) and reject a missing non-null String property like the Kotlin module does,
// so that parsing fails and the fallback parser is used

@JacksonXmlRootElement(localName = "testsuites")
record TestSuites(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testsuite")
    List<TestSuite> testsuite
) {
    TestSuites {
        if (testsuite == null) {
            testsuite = List.of();
        }
    }
}

record TestSuite(
    // Individual test cases are grouped within a suite.
    @JacksonXmlProperty(isAttribute = true)
    String name,
    @JacksonXmlProperty(isAttribute = true)
    String timestamp,
    @JacksonXmlProperty(isAttribute = true)
    String hostname,
    @JacksonXmlProperty(isAttribute = true)
    int tests,
    @JacksonXmlProperty(isAttribute = true)
    int failures,
    @JacksonXmlProperty(isAttribute = true)
    int errors,
    @JacksonXmlProperty(isAttribute = true)
    double time,
    @JacksonXmlProperty(isAttribute = true)
    int id,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testcase")
    List<TestCase> testcase,
    @JacksonXmlProperty(localName = "system-out")
    @Nullable Object systemOut,
    @JacksonXmlProperty(localName = "system-err")
    @Nullable Object systemErr,
    @JacksonXmlProperty(isAttribute = true, localName = "package")
    @Nullable String pkg,
    @Nullable Object properties
) {
    TestSuite {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(hostname, "hostname");
        if (testcase == null) {
            testcase = List.of();
        }
    }
}

record TestCase(
    // Name of the test case, typically method name.
    @JacksonXmlProperty(isAttribute = true)
    String name,
    // Class name corresponding to the test case.
    @JacksonXmlProperty(isAttribute = true)
    String classname,
    // Time value included with the test case.
    @JacksonXmlProperty(isAttribute = true)
    double time,
    @JacksonXmlProperty(isAttribute = true)
    @Nullable String file,
    @JacksonXmlProperty(isAttribute = true)
    @Nullable Integer line,
    // One of the following will be included if test did not pass.
    @JacksonXmlProperty(localName = "error")
    @Nullable TestResultDetail error,
    @JacksonXmlProperty(localName = "failure")
    @Nullable TestResultDetail failure,
    @JacksonXmlProperty(localName = "skipped")
    @Nullable TestResultDetail skipped
) {
    TestCase {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(classname, "classname");
    }
}

// This is a regular class due to limitations deserializing plain xml tag contents.
// https://github.com/FasterXML/jackson-dataformat-xml/issues/615
// consulo: public fields named like the XML properties, Jackson binds them by name
class TestResultDetail {
    // Shortened error message, as provided by the test framework.
    @JacksonXmlProperty(isAttribute = true)
    public @Nullable String message = null;

    // Error type information.
    // This typically gives the class name of the error, but may be absent or used for a similar alternative value.
    @JacksonXmlProperty(isAttribute = true)
    public @Nullable String type = null;

    // Content between the tags, which typically includes the full error stack trace.
    @JacksonXmlText(value = true)
    @JacksonXmlProperty(localName = "")
    public @Nullable String content = null;
}

public class TestXmlParser {
    private final BspClientTestNotifier myBspClientTestNotifier;
    private final FallbackTestXmlParser myFallbackTestXmlParser;

    public TestXmlParser(BspClientTestNotifier bspClientTestNotifier) {
        myBspClientTestNotifier = bspClientTestNotifier;
        myFallbackTestXmlParser = new FallbackTestXmlParser(bspClientTestNotifier);
    }

    /**
     * Processes a test result xml file, reporting suite and test case results as task start and finish notifications.
     * Parent-child relationship is identified within each suite based on the TaskId.
     *
     * @param parentId TaskId associated with the test execution.
     * @param testXml  Uri corresponding to the test result xml file to be processed.
     * @return how many test suites were handed to the client (each is expected to emit at least one
     * event). Zero means nothing was reported — e.g. an empty `<testsuites>`, every suite skipped, or
     * the file could not be read/parsed — so the caller may report a target-level result instead.
     */
    public int parseAndReport(TaskId parentId, Path testXml, @Nullable TargetKind targetKind) {
        TestSuites testSuites = parseTestXml(testXml, TestSuites.class);
        if (testSuites != null) {
            int sum = 0;
            for (TestSuite it : testSuites.testsuite()) {
                sum += processSuite(parentId, it, targetKind);
            }
            return sum;
        }
        FallbackTestXmlParser.IncompleteTestSuites fallbackTestSuites =
            parseTestXml(testXml, FallbackTestXmlParser.IncompleteTestSuites.class);
        if (fallbackTestSuites == null) {
            return 0;
        }
        List<FallbackTestXmlParser.IncompleteTestSuite> fallbackSuites = fallbackTestSuites.testsuite();
        fallbackSuites.forEach(it -> myFallbackTestXmlParser.processIncompleteInfoSuite(parentId, it, targetKind));
        return fallbackSuites.size();
    }

    /**
     * Deserialize the given test report into the TestSuites/IncompleteTestSuites type as defined above.
     */
    private <T> @Nullable T parseTestXml(Path testXml, Class<T> valueType) {
        // consulo: Jackson 3 builder; the Kotlin module is replaced by the record compact constructors above
        XmlMapper xmlMapper =
            XmlMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        // consulo: Kotlin runCatching { }.getOrNull()
        try {
            // Single empty tag does not deserialize properly, replace with empty pair.
            String rawContent = Files.readString(testXml).replace("<skipped />", "<skipped></skipped>");
            return xmlMapper.readValue(rawContent, valueType);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert each TestSuite into a series of taskStart and taskFinish notification to the client.
     * The `parents` field in each notification's TaskId will be used to indicate the parent-child relationship.
     * <p>
     * Test suite is a collection of test cases (usually a test class).
     * If the number of tests inside is not zero, but no test case results are found, it means the test suite was filtered out by Bazel -
     * this happens when the user chooses to run only one test class, not the whole Bazel target.
     * In that case, there is no point in showing such an empty suite to the user.
     *
     * @param parentId TaskId of the parent test suite or test case.
     * @param suite    TestSuite to be processed.
     * @return 1 if the suite was reported, 0 if it was skipped (the caller may then report a
     * target-level result instead).
     */
    private int processSuite(TaskId parentId, TestSuite suite, @Nullable TargetKind targetKind) {
        TestStatus suiteStatus;
        if (suite.tests() > 0 && suite.testcase().isEmpty()) {
            return 0;
        }
        else if (suite.failures() > 0) {
            suiteStatus = TestStatus.FAILED;
        }
        else if (suite.errors() > 0) {
            suiteStatus = TestStatus.FAILED;
        }
        else {
            suiteStatus = TestStatus.PASSED;
        }

        TaskId suiteTaskId = parentId.subTask(suite.name());
        Object systemErr = suite.systemErr();
        JUnitStyleTestSuiteData suiteData =
            new JUnitStyleTestSuiteData(suite.time(), null, systemErr != null ? systemErr.toString() : null);

        BazelTestDetails testDetails = BazelTestDetails.testSuite(suite.name()).build();
        myBspClientTestNotifier.startTest(testDetails, suiteTaskId, targetKind);
        suite.testcase().forEach(testCase ->
            processTestCase(suiteTaskId, suite.name(), testCase, targetKind)
        );
        myBspClientTestNotifier.finishTest(
            suite.name(),
            suiteTaskId,
            suiteStatus,
            // consulo: Kotlin Any?.toString() gives "null" for null
            String.valueOf(suite.systemOut()),
            suiteData
        );
        return 1;
    }

    /**
     * Convert a TestCase into a taskStart and taskFinish notification to the client.
     * The test case will be associated with its parent suite.
     *
     * @param parentId String identifying the parent's TaskId. Used to indicate the proper tree structure.
     * @param testCase TestCase to be processed and sent to the client.
     */
    private void processTestCase(
        TaskId parentId,
        String parentSuiteName,
        TestCase testCase,
        @Nullable TargetKind targetKind
    ) {
        TaskId testCaseTaskId = parentId.subTask(UUID.randomUUID().toString());

        TestResultDetail error = testCase.error();
        TestResultDetail failure = testCase.failure();
        TestResultDetail skipped = testCase.skipped();

        // Extract the error summary message.
        String outcomeMessage;
        if (error != null) {
            outcomeMessage = error.message;
        }
        else if (failure != null) {
            outcomeMessage = failure.message;
        }
        else if (skipped != null) {
            outcomeMessage = skipped.message;
        }
        else {
            outcomeMessage = null;
        }

        // Extract the full error message content.
        String fullOutput;
        if (error != null) {
            fullOutput = error.content;
        }
        else if (failure != null) {
            fullOutput = failure.content;
        }
        else if (skipped != null) {
            fullOutput = skipped.content;
        }
        else {
            fullOutput = "";
        }

        // Map the outcome into a TestStatus value.
        TestStatus testStatusOutcome;
        if (error != null) {
            testStatusOutcome = TestStatus.FAILED;
        }
        else if (failure != null) {
            testStatusOutcome = TestStatus.FAILED;
        }
        else if (skipped != null) {
            testStatusOutcome = TestStatus.SKIPPED;
        }
        else {
            testStatusOutcome = TestStatus.PASSED;
        }

        // Extract error type information if provided.
        String errorType;
        if (error != null) {
            errorType = error.type;
        }
        else if (failure != null) {
            errorType = failure.type;
        }
        else {
            errorType = null;
        }
        JUnitStyleTestCaseData testCaseData =
            new JUnitStyleTestCaseData(
                testCase.time(),
                testCase.classname(),
                outcomeMessage,
                fullOutput,
                errorType
            );

        BazelTestDetails testDetails =
            BazelTestDetails
                .testCase(testCase.name())
                .withClassname(testCase.classname())
                .withParentSuite(parentSuiteName)
                .withFileAndZeroBasedLine(testCase.file(), testCase.line())
                .build();
        myBspClientTestNotifier.startAndFinishTest(
            testDetails,
            testCaseTaskId,
            testStatusOutcome,
            "",
            targetKind,
            testCaseData
        );
    }

    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface IncompleteTestSuiteParser {
        boolean processTestOutput(BspClientTestNotifier bspClientTestNotifier, TaskId parentId, String systemOut);

        // consulo: upstream ep, served by the Consulo extension point
        static ExtensionPoint<IncompleteTestSuiteParser> getEp() {
            return Application.get().getExtensionPoint(IncompleteTestSuiteParser.class);
        }
    }
}

/**
 * Bazel has a separate way of parsing JUnit4 and JUnit5 test results into a xml file, resulting in
 * incomplete data about the latter.
 **/
class FallbackTestXmlParser {
    private final BspClientTestNotifier myBspClientTestNotifier;

    FallbackTestXmlParser(BspClientTestNotifier bspClientTestNotifier) {
        myBspClientTestNotifier = bspClientTestNotifier;
    }

    @JacksonXmlRootElement(localName = "testsuites")
    record IncompleteTestSuites(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "testsuite")
        List<IncompleteTestSuite> testsuite
    ) {
        IncompleteTestSuites {
            if (testsuite == null) {
                testsuite = List.of();
            }
        }
    }

    record IncompleteTestSuite(
        @JacksonXmlProperty(isAttribute = true)
        String name,
        @JacksonXmlProperty(isAttribute = true)
        int failures,
        @JacksonXmlProperty(isAttribute = true)
        int errors,
        @JacksonXmlProperty(localName = "system-out")
        @Nullable String systemOut,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "testcase")
        List<IncompleteTestCase> testcase
    ) {
        IncompleteTestSuite {
            Objects.requireNonNull(name, "name");
            if (testcase == null) {
                testcase = List.of();
            }
        }
    }

    record IncompleteTestCase(
        @JacksonXmlProperty(isAttribute = true)
        String name,
        @JacksonXmlProperty(localName = "error")
        @Nullable TestResultDetail error,
        @JacksonXmlProperty(localName = "failure")
        @Nullable TestResultDetail failure,
        @JacksonXmlProperty(localName = "skipped")
        @Nullable TestResultDetail skipped,
        @JacksonXmlProperty(localName = "time")
        @Nullable Double time,
        @JacksonXmlProperty(localName = "system-out")
        @Nullable String systemOut
    ) {
        IncompleteTestCase {
            Objects.requireNonNull(name, "name");
        }
    }

    void processIncompleteInfoSuite(TaskId parentId, IncompleteTestSuite suite, @Nullable TargetKind targetKind) {
        String systemOut = suite.systemOut();
        if (systemOut != null) {
            for (TestXmlParser.IncompleteTestSuiteParser parser : TestXmlParser.IncompleteTestSuiteParser.getEp().getExtensionList()) {
                if (parser.processTestOutput(myBspClientTestNotifier, parentId, systemOut)) {
                    return;
                }
            }
        }
        defaultIncompleteInfoSuiteProcessing(parentId, suite, targetKind);
    }

    // A Bazel target is represented by a test suite containing one test case
    private void defaultIncompleteInfoSuiteProcessing(TaskId parentId, IncompleteTestSuite suite, @Nullable TargetKind targetKind) {
        TaskId suiteTaskId = parentId.subTask(suite.name());
        TestStatus suiteStatus;
        if (suite.failures() > 0) {
            suiteStatus = TestStatus.FAILED;
        }
        else if (suite.errors() > 0) {
            suiteStatus = TestStatus.FAILED;
        }
        else {
            suiteStatus = TestStatus.PASSED;
        }
        JUnitStyleTestSuiteData testSuiteData = new JUnitStyleTestSuiteData(null, suite.systemOut(), null);

        BazelTestDetails testDetails = BazelTestDetails.testSuite(suite.name()).build();
        myBspClientTestNotifier.startTest(testDetails, suiteTaskId, targetKind);
        String fallbackMessage = suite.testcase().size() == 1 ? suite.systemOut() : null;
        suite.testcase().forEach(testCase -> {
            String testCaseSystemOut = testCase.systemOut();
            processIncompleteInfoCase(
                testCase,
                suiteTaskId,
                suite.name(),
                testCaseSystemOut != null ? testCaseSystemOut : fallbackMessage,
                targetKind
            );
        });
        myBspClientTestNotifier.finishTest(
            suite.name(),
            suiteTaskId,
            suiteStatus,
            null,
            testSuiteData
        );
    }

    /**
     * Converts a TestCase into a testStart and a testFinish events.
     */
    private void processIncompleteInfoCase(
        IncompleteTestCase testCase,
        TaskId parentId,
        String parentSuiteName,
        @Nullable String systemOut,
        @Nullable TargetKind targetKind
    ) {
        TaskId testCaseTaskId = parentId.subTask(UUID.randomUUID().toString());

        TestResultDetail error = testCase.error();
        TestResultDetail failure = testCase.failure();
        TestResultDetail skipped = testCase.skipped();

        // Extract the error summary message.
        String outcomeMessage;
        if (error != null) {
            outcomeMessage = error.message;
        }
        else if (failure != null) {
            outcomeMessage = failure.message;
        }
        else if (skipped != null) {
            outcomeMessage = skipped.message;
        }
        else {
            outcomeMessage = null;
        }
        TestStatus testStatus;
        if (error != null) {
            testStatus = TestStatus.FAILED;
        }
        else if (failure != null) {
            testStatus = TestStatus.FAILED;
        }
        else if (skipped != null) {
            testStatus = TestStatus.SKIPPED;
        }
        else {
            testStatus = TestStatus.PASSED;
        }
        String fullOutput;
        if (error != null) {
            fullOutput = error.content;
        }
        else if (failure != null) {
            fullOutput = failure.content;
        }
        else if (skipped != null) {
            fullOutput = skipped.content;
        }
        else {
            fullOutput = systemOut;
        }
        String errorType;
        if (error != null) {
            errorType = error.type;
        }
        else if (failure != null) {
            errorType = failure.type;
        }
        else {
            errorType = null;
        }
        List<String> messageParts = new ArrayList<>();
        if (outcomeMessage != null) {
            messageParts.add(outcomeMessage);
        }
        if (systemOut != null) {
            messageParts.add(systemOut);
        }
        String message = String.join("\n", messageParts);

        JUnitStyleTestCaseData testCaseData = new JUnitStyleTestCaseData(testCase.time(), testCase.name(), message, fullOutput, errorType);

        // In the generated xml, suite name and test case name are the same, but in the Test Console test names have
        // to be unique
        String testCaseName = testCase.name().substring(testCase.name().lastIndexOf('/') + 1);

        BazelTestDetails testDetails = BazelTestDetails.testCase(testCaseName).withParentSuite(parentSuiteName).build();
        myBspClientTestNotifier.startAndFinishTest(
            testDetails,
            testCaseTaskId,
            testStatus,
            message,
            targetKind,
            testCaseData
        );
    }
}
