package org.jetbrains.bazel.util;

import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.testing.BazelTestDetails;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.TaskFinishParams;
import org.jetbrains.bsp.protocol.TaskFinishParams.JUnitStyleTestCaseData;
import org.jetbrains.bsp.protocol.TaskFinishParams.TestFinish;
import org.jetbrains.bsp.protocol.TaskFinishParams.TestFinishData;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskStartParams;
import org.jetbrains.bsp.protocol.TaskStartParams.TestStart;
import org.jetbrains.bsp.protocol.TaskStartParams.TestTask;
import org.jetbrains.bsp.protocol.TestStatus;
import org.jspecify.annotations.Nullable;

public class BspClientTestNotifier {
    private final BazelTaskEventsHandler myTaskEventsHandler;

    private int myPassedTests = 0;
    private int myFailedTests = 0;
    private int myIgnoredTests = 0;
    private int myCancelledTests = 0;
    private int mySkippedTests = 0;

    public BspClientTestNotifier(BazelTaskEventsHandler taskEventsHandler) {
        myTaskEventsHandler = taskEventsHandler;
    }

    /**
     * Notifies the client about starting a single test or a test suite
     */
    public void startTest(
        BazelTestDetails testDetails,
        TaskId taskId,
        @Nullable TargetKind targetKind
    ) {
        String displayName = testDetails.displayName();
        @Nullable String locationHint = BazelTestLocationHintProvider.getLocationHint(testDetails, targetKind);
        TestStart testStart = new TestStart(displayName, testDetails.isSuite(), locationHint);
        TaskStartParams taskStartParams =
            new TaskStartParams(
                taskId,
                null,
                "Test " + displayName + " started",
                testStart
            );
        myTaskEventsHandler.onBuildTaskStart(taskStartParams);
    }

    // consulo: overload for the upstream default argument
    public void finishTest(
        String displayName,
        TaskId taskId,
        TestStatus status,
        @Nullable String message
    ) {
        finishTest(displayName, taskId, status, message, null);
    }

    /**
     * Notifies the client about finishing a single test or a test suite.
     *
     * @param displayName display name of the finished test / test suite
     * @param taskId      TaskId of the finished test / test suite
     * @param status      status of the performed test (does not matter for test suites)
     * @param message     additional message concerning the test execution
     * @param data
     */
    public void finishTest(
        String displayName,
        TaskId taskId,
        TestStatus status,
        @Nullable String message,
        @Nullable TestFinishData data
    ) {
        TestFinish testFinish = new TestFinish(displayName, status, message, null, data);

        // For leaf tests, update reported counters
        if (data instanceof JUnitStyleTestCaseData) {
            switch (status) {
                case PASSED -> myPassedTests++;
                case FAILED -> myFailedTests++;
                case IGNORED -> myIgnoredTests++;
                case CANCELLED -> myCancelledTests++;
                case SKIPPED -> mySkippedTests++;
            }
        }

        TaskFinishParams taskFinishParams =
            new TaskFinishParams(
                taskId,
                null,
                "Test " + displayName + " finished",
                BazelStatus.SUCCESS,
                testFinish
            );
        myTaskEventsHandler.onBuildTaskFinish(taskFinishParams);
    }

    // consulo: overload for the upstream default argument
    public void startAndFinishTest(
        BazelTestDetails testDetails,
        TaskId taskId,
        TestStatus status,
        @Nullable String message,
        @Nullable TargetKind targetKind
    ) {
        startAndFinishTest(testDetails, taskId, status, message, targetKind, null);
    }

    public void startAndFinishTest(
        BazelTestDetails testDetails,
        TaskId taskId,
        TestStatus status,
        @Nullable String message,
        @Nullable TargetKind targetKind,
        @Nullable TestFinishData data
    ) {
        startTest(testDetails, taskId, targetKind);
        finishTest(testDetails.displayName(), taskId, status, message, data);
    }

    /**
     * Notifies the client about beginning the testing procedure
     *
     * @param targetIdentifier identifier of the testing target being executed
     * @param taskId           TaskId of the testing target execution
     */
    public void beginTestTarget(Label targetIdentifier, TaskId taskId) {
        TestTask testingBegin = new TestTask(targetIdentifier);
        TaskStartParams taskStartParams =
            new TaskStartParams(
                taskId,
                null,
                "Test started for target " + targetIdentifier,
                testingBegin
            );
        myTaskEventsHandler.onBuildTaskStart(taskStartParams);
    }
}
