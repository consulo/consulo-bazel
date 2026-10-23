package org.jetbrains.bsp.protocol;

public interface BazelTaskEventsHandler {
    void onBuildTaskStart(TaskStartParams params);

    void onBuildTaskFinish(TaskFinishParams params);

    void onBuildLogMessage(LogMessageParams params);

    void onBuildPublishDiagnostics(PublishDiagnosticsParams params);

    void onPublishCoverageReport(CoverageReport report);

    void onCachedTestLog(CachedTestLog testLog);

    /**
     * Reports metrics derived from a finished Bazel invocation (analysis-cache discards, action cache hits).
     * Called once per invocation. Default no-op so handlers that don't care about telemetry need not implement it.
     */
    default void onBazelInvocationMetrics(BazelInvocationMetrics metrics) {
    }

    interface BazelTaskLogger {
        void info(String message);

        void message(String message);

        void warn(String message);

        void error(String errorMessage);
    }

    static BazelTaskLogger asLogger(BazelTaskEventsHandler receiver, TaskId taskId) {
        BazelTaskEventsHandler taskEventsHandler = receiver;
        return new BazelTaskLogger() {
            @Override
            public void info(String message) {
                log(MessageType.INFO, message);
            }

            @Override
            public void message(String message) {
                log(MessageType.LOG, message);
            }

            @Override
            public void warn(String message) {
                log(MessageType.WARNING, message);
            }

            @Override
            public void error(String errorMessage) {
                log(MessageType.ERROR, errorMessage);
            }

            private void log(MessageType messageType, String message) {
                String messageWithNewLine;
                if (message.endsWith("\r") || message.endsWith("\n")) {
                    messageWithNewLine = message;
                }
                else {
                    messageWithNewLine = message + '\n';
                }
                LogMessageParams params = new LogMessageParams(taskId, messageType, messageWithNewLine);
                taskEventsHandler.onBuildLogMessage(params);
            }
        };
    }
}
