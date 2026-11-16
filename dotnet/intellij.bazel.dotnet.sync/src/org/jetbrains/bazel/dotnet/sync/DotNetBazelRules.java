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
package org.jetbrains.bazel.dotnet.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService.TargetKindProvider;

import java.util.Set;

/**
 * The rules_dotnet C# rule kinds.
 * <p>
 * consulo: Consulo-only, like {@code RustBazelRules}.
 */
@ExtensionImpl
public class DotNetBazelRules implements TargetKindProvider {
    private final Set<TargetKind> myTargetKinds =
        Set.of(
            new TargetKind("csharp_library", Set.of(DotNetLanguageClass.CSHARP), RuleType.LIBRARY),
            new TargetKind("csharp_binary", Set.of(DotNetLanguageClass.CSHARP), RuleType.BINARY),
            new TargetKind("csharp_test", Set.of(DotNetLanguageClass.CSHARP), RuleType.TEST),
            new TargetKind("csharp_nunit_test", Set.of(DotNetLanguageClass.CSHARP), RuleType.TEST)
        );

    public DotNetBazelRules() {
    }

    @Override
    public Set<TargetKind> getTargetKinds() {
        return myTargetKinds;
    }
}
