package org.jetbrains.bazel.commons;

public enum BazelStatus {
    SUCCESS,
    BAD_COMMAND_LINE_ARGUMENTS,
    BUILD_ERROR,
    CANCEL,
    OOM_ERROR,
    FATAL_ERROR, // for other non-categorized errors
    ;

    // https://bazel.build/run/scripts#exit-codes
    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int BUILD_ERROR_EXIT_CODE = 1;
    private static final int BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE = 2;
    private static final int CANCEL_EXIT_CODE = 8;
    private static final int OOM_EXIT_CODE = 33;

    // specific code for bazel sync
    private static final int PARTIALLY_SUCCESS_WITH_KEEP_GOING = 3;

    public BazelStatus merge(BazelStatus anotherBazelStatus) {
        if (this == OOM_ERROR || anotherBazelStatus == OOM_ERROR) {
            // OOM errors treated specially, so preserve them.
            return OOM_ERROR;
        }
        return this.ordinal() >= anotherBazelStatus.ordinal() ? this : anotherBazelStatus;
    }

    public static BazelStatus fromExitCode(int exitCode) {
        return switch (exitCode) {
            case SUCCESS_EXIT_CODE, PARTIALLY_SUCCESS_WITH_KEEP_GOING -> SUCCESS;
            case BAD_COMMAND_LINE_ARGUMENTS_EXIT_CODE -> BAD_COMMAND_LINE_ARGUMENTS;
            case BUILD_ERROR_EXIT_CODE -> BUILD_ERROR;
            case CANCEL_EXIT_CODE -> CANCEL;
            case OOM_EXIT_CODE -> OOM_ERROR;
            default -> FATAL_ERROR;
        };
    }
}
