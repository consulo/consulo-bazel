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

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.util.BazelRuleSetProvider;
import org.jetbrains.bazel.util.BazelRuleSetProvider.BazelRuleSet;

import java.util.List;

/**
 * Enables the {@code dotnet_info} module of the aspect for workspaces that use rules_dotnet.
 * <p>
 * consulo: Consulo-only, like {@code RustBazelRuleSetPlugin}.
 */
@ExtensionImpl
public class DotNetBazelRuleSetPlugin implements BazelRuleSetProvider {
    public static final BazelRuleSet DotNetRuleSet = new BazelRuleSet(
        Rules.DOTNET,
        List.of("rules_dotnet"),
        false,
        List.of(),
        List.of("https://github.com/bazel-contrib/rules_dotnet/")
    );

    public DotNetBazelRuleSetPlugin() {
    }

    @Override
    public List<BazelRuleSet> ruleSets() {
        return List.of(DotNetRuleSet);
    }
}
