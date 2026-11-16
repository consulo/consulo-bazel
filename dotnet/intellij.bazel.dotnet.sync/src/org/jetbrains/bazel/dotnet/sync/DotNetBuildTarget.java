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

import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The .NET assembly of a rules_dotnet target ({@code DotNetAssemblyInfo} of the Consulo aspect), the build target data of the
 * workspace snapshot.
 * <p>
 * consulo: Consulo-only, upstream has no .NET support.
 *
 * @param language            {@code csharp}, {@code fsharp} or {@code vb}
 * @param targetFramework     the resolved target framework moniker, e.g. {@code net8.0}
 * @param sources             the {@code srcs} of the target, including external ones (the NUnit shim of {@code csharp_nunit_test})
 * @param frameworkReferences the reference assemblies of the targeting pack
 * @param packageReferences   the reference assemblies of direct dependencies that are no rules_dotnet compile targets (NuGet)
 * @param languageVersion     the {@code langversion} attribute, empty for the default of the toolchain
 * @param nullable            the {@code nullable} attribute
 * @param assembly            the built assembly
 * @param executable          a binary or test
 * @param dotnet              the hermetic {@code dotnet} executable of the compile action, its SDK is {@code <dir>/sdk/<version>}
 */
public record DotNetBuildTarget(
    String assemblyName,
    String language,
    String targetFramework,
    List<OutputLocation> sources,
    List<OutputLocation> frameworkReferences,
    List<OutputLocation> packageReferences,
    List<String> defines,
    String languageVersion,
    String nullable,
    boolean allowUnsafeBlocks,
    @Nullable OutputLocation assembly,
    @Nullable OutputLocation pdb,
    List<String> internalsVisibleTo,
    String projectSdk,
    boolean executable,
    @Nullable OutputLocation dotnet
) implements BuildTargetData {
    public static final String CSHARP = "csharp";

    public boolean isCSharp() {
        return CSHARP.equals(language);
    }

    public static @Nullable DotNetBuildTarget extractDotNetBuildTarget(BuildTarget target) {
        return BuildTarget.extractData(target, DotNetBuildTarget.class);
    }
}
