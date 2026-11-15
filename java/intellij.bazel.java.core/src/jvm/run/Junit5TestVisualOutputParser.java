package org.jetbrains.bazel.jvm.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.server.bep.TestXmlParser;
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaBazelRules;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bazel.util.BspClientTestNotifier;
import org.jetbrains.bsp.protocol.TaskFinishParams;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TestStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
@ExtensionImpl
public class Junit5TestVisualOutputParser implements TestXmlParser.IncompleteTestSuiteParser {
    @Override
    public boolean processTestOutput(BspClientTestNotifier bspClientTestNotifier, TaskId parentId, String systemOut) {
        if (!textContainsJunit5VisualOutput(systemOut)) {
            return false;
        }
        List<TestResultTreeNode> tree = generateTestResultTree(systemOut, parentId);
        notifyClient(bspClientTestNotifier, tree);
        return true;
    }

    /**
     * generateTestResultTree parse the junit5 test report and return the test result tree for each test target
     * <p>
     * The tree for each target is organized as follows:
     * - The root is a TestResultTreeNode where the name is the bazel test target, and its status is null.
     * - The sibling nodes of the root are the test result tree for each junit5 test classes in this test target. It can recursively
     * contain test result or a test suit result.
     * <p>
     * The sibling's taskId include its parent's taskId as parent. However, one exception is that direct sibling of root node does not have parents in taskId.
     * It is because that when we notify the client, the root node will be converted to beginTestTarget call, while other nodes are converted into startTest
     * call.
     *
     * @return List of generated TestResultTreeNode instances
     */
    private List<TestResultTreeNode> generateTestResultTree(String output, TaskId parentId) {
        // consulo: Kotlin String.lines() keeps a trailing empty line
        String[] lines = output.split("\r\n|\n|\r", -1);
        List<TestResultTreeNode> testResultTrees = new ArrayList<>();

        TestResultTreeNode treeRootForCurrentTarget = null;
        TestResultTreeNode currentNode = null;
        TestResultTreeNode currentStackTraceNode = null;

        for (String line : lines) {
            String cleanLine = removeFormat(line);
            Matcher testEndedMatcher = testingEndedPattern.matcher(line);
            Matcher testLineMatcher = testLinePattern.matcher(cleanLine);
            Matcher failuresCountMatcher = failuresCountPattern.matcher(cleanLine);
            if (treeRootForCurrentTarget == null) {
                String extractedTargetName = parseBuildTargetName(line);
                boolean testResultsAreStarting = line.indexOf(TEST_START_CHARACTER) >= 0;
                String rootNodeName;
                if (extractedTargetName != null) {
                    rootNodeName = extractedTargetName;
                }
                else if (testResultsAreStarting) {
                    rootNodeName = "";
                }
                else {
                    continue;
                }
                treeRootForCurrentTarget =
                    new TestResultTreeNode(rootNodeName, parentId.subTask(rootNodeName), null, new ArrayList<>(), -1);
                testResultTrees.add(treeRootForCurrentTarget);
                currentNode = treeRootForCurrentTarget;
            }
            else if (testEndedMatcher.find()) {
                Long time = toLongOrNull(testEndedMatcher.group("time"));
                treeRootForCurrentTarget.myTime = time;
                currentNode = null;
                treeRootForCurrentTarget = null;
                currentStackTraceNode = null;
            }
            else if (failuresCountMatcher.find()) {
                currentNode = null;
            }
            else if (testLineMatcher.find()) {
                int indent = testLineMatcher.start("name");
                TestResultTreeNode parent = currentNode == null ? null : findParentByIndent(currentNode, indent);

                Pair<String, @Nullable Long> nameAndTime = separateTimeFromTestName(testLineMatcher.group("name"));
                String testName = nameAndTime.getFirst();
                Long testTime = nameAndTime.getSecond();

                List<String> messageLines = new ArrayList<>();
                messageLines.add(testLineMatcher.group("message"));
                TestResultTreeNode newNode =
                    new TestResultTreeNode(
                        /* name = */ testName,
                        /* taskId = */ (parent != null ? parent.myTaskId : parentId).uniqueSubTask(testName),
                        /* status = */ toTestStatus(testLineMatcher.group("result")),
                        /* messageLines = */ messageLines,
                        /* indent = */ indent,
                        /* parent = */ parent,
                        /* time = */ testTime
                    );
                if (parent != null) {
                    parent.myChildren.put(newNode.myName, newNode);
                }
                currentNode = newNode;
            }
            else if (isStackTraceStartingLine(cleanLine)) {
                currentStackTraceNode = findNodeByPath(cleanLine, treeRootForCurrentTarget);
                currentNode = null;
            }
            else if (currentNode != null && !currentNode.myMessageLines.isEmpty()) {
                currentNode.myMessageLines.add(parseMultilineMessageContinuation(line, cleanLine));
            }
            else if (currentStackTraceNode != null) {
                currentStackTraceNode.myStacktrace.add(substringAfter(cleanLine, "    => "));
            }
        }
        return testResultTrees;
    }

    private void notifyClient(BspClientTestNotifier bspClientTestNotifier, List<TestResultTreeNode> trees) {
        for (TestResultTreeNode tree : trees) {
            tree.notifyClient(bspClientTestNotifier);
        }
    }

    private @Nullable String parseBuildTargetName(String line) {
        Matcher testingStartMatcher = testingStartPattern.matcher(line);
        if (testingStartMatcher.find()) {
            return testingStartMatcher.group("target");
        }
        else {
            return null;
        }
    }

    private @Nullable TestResultTreeNode findParentByIndent(TestResultTreeNode previousNode, int indent) {
        if (previousNode.myIndent < indent) {
            return previousNode;
        }
        else if (previousNode.myIndent == indent) {
            return previousNode.myParent;
        }
        else {
            TestResultTreeNode realParent = previousNode;
            while (realParent.myParent != null && realParent.myIndent >= indent) {
                realParent = realParent.myParent;
            }
            return realParent;
        }
    }

    private Pair<String, @Nullable Long> separateTimeFromTestName(String testName) {
        Matcher testNameWithTimeMatcher = testNameWithTimePattern.matcher(testName);
        if (testNameWithTimeMatcher.find()) {
            return Pair.create(testNameWithTimeMatcher.group("name"), toLongOrNull(testNameWithTimeMatcher.group("time")));
        }
        else {
            return Pair.create(testName, null);
        }
    }

    private boolean isStackTraceStartingLine(String line) {
        String it = line.trim();
        return it.startsWith("JUnit Jupiter:") || it.startsWith("JUnit Platform Suite:");
    }

    private String parseMultilineMessageContinuation(String rawLine, String cleanLine) {
        // We want to preserve leading whitespaces, where possible. We need to find the starting offset for that.
        // Some versions of JUnit 5 include the vertical bar "│" before multiline error messages, some don't.
        // For an example of a test that doesn't have the vertical bar, try to break any test using intellij_integration_test_suite.
        // We can also try to use the color information to find the message offset, but it may be different between operating systems.
        String afterBar = substringAfterOrNull(cleanLine, "│        ");
        if (afterBar != null) {
            return afterBar;
        }
        String afterColor = substringAfterOrNull(rawLine, "?[0m?[31m   ");
        if (afterColor != null) {
            return removeFormat(afterColor);
        }
        return cleanLine.stripLeading();
    }

    private static @Nullable String substringAfterOrNull(String receiver, String delimiter) {
        int index = receiver.indexOf(delimiter);
        return index == -1 ? null : receiver.substring(index + delimiter.length());
    }

    private @Nullable TestResultTreeNode findNodeByPath(String path, TestResultTreeNode root) {
        String[] segment = path.trim().split(":", -1);
        TestResultTreeNode result = root;
        for (int i = 1; i < segment.length; i++) {
            TestResultTreeNode child = result.myChildren.get(segment[i]);
            if (child == null) {
                return null;
            }
            result = child;
        }
        return result;
    }

    @SuppressWarnings("unused")
    private String testUUID() {
        return "test-" + UUID.randomUUID().toString();
    }

    private static TestStatus toTestStatus(String receiver) {
        return switch (receiver) {
            case "✔" -> TestStatus.PASSED;
            case "✘" -> TestStatus.FAILED;
            default -> TestStatus.SKIPPED;
        };
    }

    public static boolean textContainsJunit5VisualOutput(String text) {
        return text.indexOf(TEST_START_CHARACTER) >= 0;
    }

    // consulo: upstream private top-level declarations of the file

    private static final char TEST_START_CHARACTER = '╷'; // every junit5 visual output starts with that character ("╷")

    private static final Pattern testingStartPattern = Pattern.compile("^Executing\\htests\\hfrom\\h(?<target>[^:]*:[^:]+)");
    private static final Pattern testLinePattern =
        Pattern.compile("^(?:[\\h└├│]{3})+[└├│]─\\h(?<name>.+)\\h(?<result>[✔✘↷])\\h?(?<message>.*)$");
    private static final Pattern testNameWithTimePattern = Pattern.compile("^(?<name>.+)\\h(?<time>\\d+)\\hms$");
    private static final Pattern testingEndedPattern = Pattern.compile("^Test\\hrun\\hfinished\\hafter\\h(?<time>\\d+)\\hms");
    private static final Pattern failuresCountPattern = Pattern.compile("^Failures \\(\\d+\\):$");

    private static final Pattern FORMAT_PATTERN = Pattern.compile("[?\\u001b]\\[[;\\d]*m");

    static String removeFormat(String receiver) {
        // '1B' symbol appears in console output and test logs, while '?' appears in test XMLs
        return FORMAT_PATTERN.matcher(receiver).replaceAll("");
    }

    static TaskFinishParams.JUnitStyleTestCaseData createTestCaseData(String message, @Nullable Long time) {
        return new TaskFinishParams.JUnitStyleTestCaseData(
            /* time = */ time == null ? null : time / 1000.0,
            /* className = */ null,
            /* errorMessage = */ message,
            /* output = */ null,
            /* errorType = */ null
        );
    }

    // consulo: Kotlin String.toLongOrNull()
    private static @Nullable Long toLongOrNull(String text) {
        try {
            return Long.parseLong(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    // consulo: Kotlin String.substringAfter(delimiter) returns the whole string when the delimiter is missing
    private static String substringAfter(String receiver, String delimiter) {
        int index = receiver.indexOf(delimiter);
        return index == -1 ? receiver : receiver.substring(index + delimiter.length());
    }
}

class TestResultTreeNode {
    final String myName;
    final TaskId myTaskId;
    final @Nullable TestStatus myStatus;
    final List<String> myMessageLines;
    final int myIndent;
    final @Nullable TestResultTreeNode myParent;
    @Nullable Long myTime;

    final Map<String, TestResultTreeNode> myChildren = new LinkedHashMap<>();
    final List<String> myStacktrace = new ArrayList<>();

    TestResultTreeNode(
        String name,
        TaskId taskId,
        @Nullable TestStatus status,
        List<String> messageLines,
        int indent,
        @Nullable TestResultTreeNode parent,
        @Nullable Long time
    ) {
        this.myName = name;
        this.myTaskId = taskId;
        this.myStatus = status;
        this.myMessageLines = messageLines;
        this.myIndent = indent;
        this.myParent = parent;
        this.myTime = time;
    }

    // consulo: overload for the upstream default arguments (parent = null, time = null)
    TestResultTreeNode(
        String name,
        TaskId taskId,
        @Nullable TestStatus status,
        List<String> messageLines,
        int indent
    ) {
        this(name, taskId, status, messageLines, indent, null, null);
    }

    boolean isLeafNode() {
        return myChildren.isEmpty();
    }

    boolean isRootNode() {
        return myParent == null;
    }

    void notifyClient(BspClientTestNotifier bspClientTestNotifier) {
        List<String> parentSuiteNames = getAncestorSuiteNamesExcludingRoot();
        if (isRootNode()) {
            bspClientTestNotifier.beginTestTarget(Label.parse(myName), myTaskId);
            for (TestResultTreeNode it : myChildren.values()) {
                it.notifyClient(bspClientTestNotifier);
            }
        }
        else if (isLeafNode()) {
            String fullMessage = generateMessage();

            BazelTestDetails testDetails = BazelTestDetails.testCase(myName).withParentSuites(parentSuiteNames).build();
            bspClientTestNotifier.startTest(testDetails, myTaskId, JavaBazelRules.JAVA_TEST_KIND);

            if (myStatus == TestStatus.FAILED && myParent != null && myParent.isRootNode() && myChildren.isEmpty()) {
                // BAZEL-2080: if an exception happens at the start of a test suit, there will be no test case run
                // and no test case reported. Teamcity will mark a testsuit with no test case as success.
                // So in this case, we need to report a dummy test case with TestStatus.FAILED status.
                String displayName = "no tests found";
                TaskId placeholderID = myTaskId.subTask("placeholder");

                BazelTestDetails placeholderTestDetails = BazelTestDetails.testCase(displayName).withParentSuites(parentSuiteNames).build();
                bspClientTestNotifier.startAndFinishTest(
                    /* testDetails = */ placeholderTestDetails,
                    /* taskId = */ placeholderID,
                    /* status = */
                    TestStatus.FAILED,
                    /* message = */ displayName,
                    /* targetKind = */ JavaBazelRules.JAVA_TEST_KIND,
                    /* data = */ Junit5TestVisualOutputParser.createTestCaseData(displayName, myTime)
                );
            }
            // this should never be null, because every node except root nodes should have status. Just for compiling
            if (myStatus == null) {
                return;
            }
            bspClientTestNotifier.finishTest(
                /* displayName = */ myName,
                /* taskId = */ myTaskId,
                /* status = */
                myStatus,
                /* message = */ fullMessage,
                /* data = */ Junit5TestVisualOutputParser.createTestCaseData(fullMessage, myTime)
            );
        }
        else {
            BazelTestDetails testDetails = BazelTestDetails.testSuite(myName).withParentSuites(parentSuiteNames).build();
            bspClientTestNotifier.startTest(testDetails, myTaskId, JavaBazelRules.JAVA_TEST_KIND);
            for (TestResultTreeNode it : myChildren.values()) {
                it.notifyClient(bspClientTestNotifier);
            }
            // this should never be null, because every node except root nodes should have status. Just for compiling
            if (myStatus == null) {
                return;
            }
            bspClientTestNotifier.finishTest(
                /* displayName = */ myName,
                /* taskId = */ myTaskId,
                /* status = */
                myStatus,
                /* message = */ joinLinesIgnoringLastEmpty(myMessageLines)
            );
        }
    }

    private List<String> getAncestorSuiteNamesExcludingRoot() {
        List<String> result = new ArrayList<>();
        for (TestResultTreeNode it = myParent; it != null; it = it.myParent) {
            result.add(it.myName);
        }
        if (!result.isEmpty()) {
            result.remove(result.size() - 1);
        }
        Collections.reverse(result);
        return result;
    }

    private String generateMessage() {
        return joinLinesIgnoringLastEmpty(myMessageLines) + "\n" + joinLinesIgnoringLastEmpty(myStacktrace);
    }

    private static String joinLinesIgnoringLastEmpty(List<String> receiver) {
        List<String> linesToJoin =
            !receiver.isEmpty() && receiver.get(receiver.size() - 1).isEmpty()
                ? receiver.subList(0, receiver.size() - 1)
                : receiver;
        return String.join("\n", linesToJoin);
    }
}
