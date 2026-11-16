package org.jetbrains.bazel.sync;

import consulo.util.lang.ControlFlowException;

/**
 * Stops the sync cleanly when the error is expected.
 * Before it throws this exception, the code shows the reason as a diagnostic.
 * For example, the `.bazelproject` file has errors, and the user must fix them.
 * The sync stops and does not log an error.
 */
// consulo: a RuntimeException (upstream: Exception), Kotlin has no checked exceptions and Consulo ControlFlowException.rethrow
// expects a RuntimeException
public class ProjectSyncAbortException extends RuntimeException implements ControlFlowException {
}
