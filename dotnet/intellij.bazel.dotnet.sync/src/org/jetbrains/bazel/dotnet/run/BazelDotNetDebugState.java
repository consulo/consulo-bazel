/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.dotnet.run;

import consulo.execution.configuration.CommandLineState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ProcessHandler;
import org.jetbrains.bazel.run.state.GenericRunConfigurationState;

/**
 * The state of a debug run: only the settings of the run configuration, {@link BazelDotNetDebugRunner} builds the target and
 * starts the debug session.
 * <p>
 * consulo: Consulo-only, like {@code CcDebugCommandLineState}.
 */
public class BazelDotNetDebugState extends CommandLineState {
    private final GenericRunConfigurationState myBazelState;

    public BazelDotNetDebugState(ExecutionEnvironment environment, GenericRunConfigurationState bazelState) {
        super(environment);
        myBazelState = bazelState;
    }

    public GenericRunConfigurationState getBazelState() {
        return myBazelState;
    }

    @Override
    protected ProcessHandler startProcess() {
        throw new UnsupportedOperationException("the debug session is started by BazelDotNetDebugRunner");
    }
}
