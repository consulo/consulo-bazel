package org.jetbrains.bazel.bazelrunner;

import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector;
import org.jetbrains.bazel.commons.BazelStatus;

import java.util.List;

public class BazelProcessResult {
    private final OutputCollector myStdoutCollector;
    private final OutputCollector myStderrCollector;
    private final int myExitCode;

    public BazelProcessResult(
        OutputCollector stdoutCollector,
        OutputCollector stderrCollector,
        int exitCode
    ) {
        myStdoutCollector = stdoutCollector;
        myStderrCollector = stderrCollector;
        myExitCode = exitCode;
    }

    public int getExitCode() {
        return myExitCode;
    }

    public BazelStatus getBazelStatus() {
        return BazelStatus.fromExitCode(myExitCode);
    }

    public boolean isSuccess() {
        return getBazelStatus() == BazelStatus.SUCCESS;
    }

    public boolean isNotSuccess() {
        return !isSuccess();
    }

    public List<String> getStdoutLines() {
        return myStdoutCollector.lines();
    }

    public byte[] getStdout() {
        return myStdoutCollector.raw();
    }

    public List<String> getStderrLines() {
        return myStderrCollector.lines();
    }

    public byte[] getStderr() {
        return myStderrCollector.raw();
    }
}
