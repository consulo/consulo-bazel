package org.jetbrains.bazel.run.state;

// consulo: upstream GenericRunState.kt also declares GenericRunConfigurationState and AbstractGenericRunState; Java cannot nest
// them here (GenericRunState extends AbstractGenericRunState, a cyclic inheritance), so they have their own files
public class GenericRunState extends AbstractGenericRunState<GenericRunState> {
}
