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
package consulo.bazel.dotnet.workspace;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.content.layer.ModifiableRootModel;
import org.jetbrains.bazel.dotnet.sync.DotNetBuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget;

/**
 * Configures a C# module of the Bazel sync after {@code DotNetBazelWorkspaceImporter} has set it up (the {@code bazel-dotnet-core}
 * and {@code bazel-csharp} extensions, content roots, dependencies and reference libraries), e.g. enables the module extension of
 * a test framework the target references.
 * <p>
 * Modules are imported again on every sync, so an implementation also disables what it enabled when the target no longer needs it.
 * Called under a read action on a pool thread.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelDotNetModuleConfigurator {
    void configure(ModifiableRootModel model, BuildTarget target, DotNetBuildTarget dotNetTarget);
}
