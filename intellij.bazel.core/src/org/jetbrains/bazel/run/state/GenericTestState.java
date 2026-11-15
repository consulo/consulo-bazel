package org.jetbrains.bazel.run.state;

// consulo: upstream GenericTestState.kt also declares AbstractGenericTestState; Java cannot nest it here (GenericTestState extends
// AbstractGenericTestState, a cyclic inheritance), so it has its own file
public class GenericTestState extends AbstractGenericTestState<GenericTestState> {
}
