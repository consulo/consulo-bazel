package org.jetbrains.bazel.run.task;

import consulo.execution.test.sm.ServiceMessageBuilder;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputType;
import consulo.process.util.AnsiEscapeDecoder;
import org.jetbrains.bazel.commons.BazelStatus;
import org.jetbrains.bazel.taskEvents.BazelTaskListener;
import org.jetbrains.bsp.protocol.TaskFinishParams.JUnitStyleTestCaseData;
import org.jetbrains.bsp.protocol.TaskFinishParams.JUnitStyleTestSuiteData;
import org.jetbrains.bsp.protocol.TaskFinishParams.TestFinish;
import org.jetbrains.bsp.protocol.TaskId;
import org.jetbrains.bsp.protocol.TaskStartParams.TestStart;
import org.jetbrains.bsp.protocol.TaskStartParams.TestTask;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BazelTestTaskListener implements BazelTaskListener {
    private final ProcessHandler myHandler;
    private final @Nullable Consumer<Path> myCoverageReportListener;
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public BazelTestTaskListener(ProcessHandler handler, @Nullable Consumer<Path> coverageReportListener) {
        myHandler = handler;
        myCoverageReportListener = coverageReportListener;
    }

    public BazelTestTaskListener(ProcessHandler handler) {
        this(handler, null);
    }

    private final ConcurrentHashMap<TaskId, TestStart> myTestStartData = new ConcurrentHashMap<>();

    private @Nullable TaskId findParentTaskId(TaskId taskId) {
        TaskId id = taskId.parent();
        while (id != null) {
            if (myTestStartData.containsKey(id)) {
                return id;
            }
            id = id.parent();
        }
        return null;
    }

    @Override
    public void onTaskStart(
        TaskId taskId,
        String message,
        @Nullable Object data
    ) {
        if (data instanceof TestTask) {
            // OutputToGeneralTestEventsConverter.MyServiceMessageVisitor.visitServiceMessage ignores the first testingStarted event
            myHandler.notifyTextAvailable(
                toStringWithNewline(new ServiceMessageBuilder("testingStarted").toString()),
                ProcessOutputType.STDOUT
            );
            myHandler.notifyTextAvailable(
                toStringWithNewline(new ServiceMessageBuilder("testingStarted").toString()),
                ProcessOutputType.STDOUT
            );
        }
        else if (data instanceof TestStart testStart) {
            myTestStartData.put(taskId, testStart);
            TaskId parentTaskId = findParentTaskId(taskId);
            String serviceMessage;
            if (testStart.isSuit()) {
                serviceMessage = addNodeId(ServiceMessageBuilder.testSuiteStarted(testStart.displayName()), taskId.id())
                    .addAttribute("parentNodeId", parentTaskId != null ? parentTaskId.id() : "0")
                    .addAttribute("locationHint", testStart.locationHint())
                    .toString();
            }
            else {
                serviceMessage = addNodeId(ServiceMessageBuilder.testStarted(testStart.displayName()), taskId.id())
                    .addAttribute("parentNodeId", parentTaskId != null ? parentTaskId.id() : "0")
                    .addAttribute("locationHint", testStart.locationHint())
                    .toString();
            }
            myHandler.notifyTextAvailable(toStringWithNewline(serviceMessage), ProcessOutputType.STDOUT);
        }
    }

    @Override
    public void onTaskFinish(
        TaskId taskId,
        String message,
        BazelStatus status,
        @Nullable Object data
    ) {
        TestStart startData = myTestStartData.remove(taskId);

        if (data instanceof TestFinish testFinish) {
            ServiceMessageBuilder serviceMessage;
            if (startData != null && startData.isSuit()) {
                serviceMessage = processTestSuiteFinish(taskId, testFinish);
            }
            else {
                serviceMessage = processTestCaseFinish(taskId, testFinish);
            }

            myHandler.notifyTextAvailable(toStringWithNewline(serviceMessage.toString()), ProcessOutputType.STDOUT);
        }
    }

    @Override
    public void onOutputStream(@Nullable TaskId taskId, String text) {
        myAnsiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }

    @Override
    public void onErrorStream(@Nullable TaskId taskId, String text) {
        myAnsiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }

    // For compatibility with older BSP servers
    // TODO: Log messages in the correct place
    @Override
    public void onLogMessage(TaskId taskId, String message) {
        String messageWithNewline = toStringWithNewline(message);
        myAnsiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT, (s, key) -> {
            myHandler.notifyTextAvailable(s, key);
        });
    }

    @Override
    public void onPublishCoverageReport(Path coverageReport) {
        Consumer<Path> coverageReportListener = myCoverageReportListener;
        if (coverageReportListener != null) {
            coverageReportListener.accept(coverageReport);
        }
    }

    private void checkTestStatus(
        TaskId taskId,
        TestFinish data,
        @Nullable JUnitStyleTestCaseData details
    ) {
        ServiceMessageBuilder failureMessageBuilder =
            switch (data.status()) {
                case FAILED -> ServiceMessageBuilder.testFailed(data.displayName());

                case CANCELLED -> ServiceMessageBuilder.testIgnored(data.displayName());

                case IGNORED -> ServiceMessageBuilder.testIgnored(data.displayName());

                case SKIPPED -> ServiceMessageBuilder.testIgnored(data.displayName());

                default -> null;
            };

        if (failureMessageBuilder != null) {
            // if the error message is empty or blank, IntelliJ will see the test case as successful
            String detailsErrorMessage = details != null ? details.errorMessage() : null;
            String errorMessage = detailsErrorMessage != null && !detailsErrorMessage.isBlank() ? detailsErrorMessage : "Failed";
            ServiceMessageBuilder builder = addMessage(addNodeId(failureMessageBuilder, taskId.id()), errorMessage);
            String errorType = details != null ? details.errorType() : null;
            if (errorType != null) {
                builder.addAttribute("type", errorType);
            }
            myHandler.notifyTextAvailable(toStringWithNewline(failureMessageBuilder.toString()), ProcessOutputType.STDOUT);
            String output = details != null ? details.output() : null;
            if (output != null) {
                myHandler.notifyTextAvailable(output, ProcessOutputType.STDERR);
            }
        }
        else {
            String output = details != null ? details.output() : null;
            if (output != null) {
                myHandler.notifyTextAvailable(output, ProcessOutputType.STDOUT);
            }
        }
    }

    private ServiceMessageBuilder processTestCaseFinish(TaskId taskId, TestFinish data) {
        JUnitStyleTestCaseData details = extractTestFinishData(data, JUnitStyleTestCaseData.class);

        checkTestStatus(taskId, data, details);

        return addTime(
            addMessage(addNodeId(ServiceMessageBuilder.testFinished(data.displayName()), taskId.id()), data.message()),
            details != null ? details.time() : null
        );
    }

    private ServiceMessageBuilder processTestSuiteFinish(TaskId taskId, TestFinish data) {
        JUnitStyleTestSuiteData details = extractTestFinishData(data, JUnitStyleTestSuiteData.class);

        String systemOut = details != null ? details.systemOut() : null;
        if (systemOut != null) {
            myHandler.notifyTextAvailable(systemOut, ProcessOutputType.STDOUT);
        }
        String systemErr = details != null ? details.systemErr() : null;
        if (systemErr != null) {
            myHandler.notifyTextAvailable(systemErr, ProcessOutputType.STDERR);
        }
        return addTime(
            addNodeId(ServiceMessageBuilder.testSuiteFinished(data.displayName()), taskId.id()),
            details != null ? details.time() : null
        );
    }

    private static ServiceMessageBuilder addNodeId(ServiceMessageBuilder receiver, String nodeId) {
        return receiver.addAttribute("nodeId", nodeId);
    }

    private static ServiceMessageBuilder addMessage(ServiceMessageBuilder receiver, @Nullable String message) {
        return message != null && !message.isEmpty() ? receiver.addAttribute("message", message) : receiver;
    }

    private static ServiceMessageBuilder addTime(ServiceMessageBuilder receiver, @Nullable Double time) {
        if (time == null || !Double.isFinite(time)) {
            return receiver;
        }
        // consulo: Kotlin `time.toDuration(DurationUnit.SECONDS).inWholeMilliseconds` (rounded to nanoseconds, then truncated)
        long nanos = Math.round(time * 1_000_000_000.0);
        return receiver.addAttribute("duration", String.valueOf(nanos / 1_000_000L));
    }

    // consulo: upstream is `inline fun <reified Data>`, the type is passed explicitly
    private static <Data> @Nullable Data extractTestFinishData(TestFinish testFinishData, Class<Data> type) {
        return type.isInstance(testFinishData.data()) ? type.cast(testFinishData.data()) : null;
    }

    /**
     * If a system message sent to the process handler does not end with a newline,
     * it might connect to another message and not be parsed correctly
     */
    private static String toStringWithNewline(String receiver) {
        return receiver.endsWith("\n") ? receiver : receiver + "\n";
    }
}
