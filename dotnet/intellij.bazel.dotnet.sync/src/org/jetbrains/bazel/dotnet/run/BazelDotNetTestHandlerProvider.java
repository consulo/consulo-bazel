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

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.dotnet.localize.BazelDotNetLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.dotnet.sync.DotNetLanguageClass;
import org.jetbrains.bazel.run.BazelRunHandler;
import org.jetbrains.bazel.run.RunHandlerProvider;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bsp.protocol.BuildTargetInfo;

import java.util.List;

// consulo: Consulo-only, like CcRunHandlerProvider
@ExtensionImpl(id = "BazelDotNetTestHandlerProvider")
public class BazelDotNetTestHandlerProvider implements RunHandlerProvider {
    @Override
    public String getId() {
        return "BazelDotNetTestHandlerProvider";
    }

    @Override
    public LocalizeValue getProvidedRunHandlerName() {
        return BazelDotNetLocalize.testHandlerDotnetName();
    }

    @Override
    public boolean isTestHandler() {
        return true;
    }

    @Override
    public BazelRunHandler createRunHandler(BazelRunConfiguration configuration) {
        return new BazelDotNetTestHandler();
    }

    @Override
    public boolean canRun(Project project, List<BuildTargetInfo> targets) {
        if (targets.size() != 1) {
            return false;
        }
        BuildTargetInfo target = targets.get(0);
        return DotNetLanguageClass.includesCSharp(target.kind()) && target.kind().ruleType() == RuleType.TEST;
    }
}
