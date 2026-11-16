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

import consulo.bazel.dotnet.localize.BazelDotNetLocalize;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState;
import org.jetbrains.bazel.run.state.GenericRunState;

/**
 * Runs a C# binary with {@code bazel run}, like the generic handler; {@link BazelDotNetDebugRunner} debugs it.
 * <p>
 * consulo: Consulo-only, like {@code CcRunHandler}.
 */
public class BazelDotNetRunHandler implements BazelRunHandler {
    private final GenericRunState myState = new GenericRunState();

    @Override
    public GenericRunState getState() {
        return myState;
    }

    @Override
    public String getName() {
        return BazelDotNetLocalize.runHandlerDotnetName().get();
    }

    @Override
    public boolean isTestHandler() {
        return false;
    }

    @Override
    public RunProfileState getRunProfileState(Executor executor, ExecutionEnvironment environment) {
        if (executor instanceof DefaultDebugExecutor) {
            return new BazelDotNetDebugState(environment, myState);
        }
        return new BazelRunCommandLineState(environment, myState);
    }
}
