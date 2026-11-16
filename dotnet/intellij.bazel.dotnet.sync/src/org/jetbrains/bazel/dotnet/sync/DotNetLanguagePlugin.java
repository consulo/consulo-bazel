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

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.DotNetAssemblyInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Maps the {@code DotNetAssemblyInfo} of the Consulo aspect ({@code modules/dotnet_info.bzl}) to {@link DotNetBuildTarget}.
 * <p>
 * consulo: Consulo-only, like {@code RustLanguagePlugin}. Only C# assemblies are mapped (TODO consulo: F# and Visual Basic).
 */
@ExtensionImpl
public class DotNetLanguagePlugin implements LanguagePlugin {
    public DotNetLanguagePlugin() {
    }

    private static @Nullable DotNetAssemblyInfo getCSharpAssemblyInfoOrNull(TargetIdeInfo receiver) {
        if (!receiver.hasDotnetAssemblyInfo()) {
            return null;
        }
        DotNetAssemblyInfo info = receiver.getDotnetAssemblyInfo();
        return DotNetBuildTarget.CSHARP.equals(info.getLanguage()) ? info : null;
    }

    @Override
    public Set<Class<? extends BuildTargetData>> getProvidedBuildTargetTypes() {
        return Set.of(DotNetBuildTarget.class);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguages() {
        return Set.of(DotNetLanguageClass.CSHARP);
    }

    @Override
    public List<LanguageClass> collectUsedLanguages(TargetIdeInfo target) {
        return getCSharpAssemblyInfoOrNull(target) != null ? List.of(DotNetLanguageClass.CSHARP) : List.of();
    }

    @Override
    public CompletableFuture<List<BuildTargetData>> mapBuildTargetData(BazelServerFacade server, TargetIdeInfo target) {
        DotNetAssemblyInfo info = getCSharpAssemblyInfoOrNull(target);
        if (info == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        // one parse call for all locations of the target (the hard links of the outputs are created together)
        List<ArtifactLocation> locations = new ArrayList<>();
        locations.addAll(info.getSourcesList());
        locations.addAll(info.getFrameworkReferencesList());
        locations.addAll(info.getPackageReferencesList());
        int sourcesEnd = info.getSourcesCount();
        int frameworkEnd = sourcesEnd + info.getFrameworkReferencesCount();
        int packageEnd = frameworkEnd + info.getPackageReferencesCount();
        ArtifactLocation assembly = info.hasAssembly() ? nonEmpty(info.getAssembly()) : null;
        ArtifactLocation pdb = info.hasPdb() ? nonEmpty(info.getPdb()) : null;
        ArtifactLocation dotnet = info.hasDotnet() ? nonEmpty(info.getDotnet()) : null;
        int assemblyIndex = addOptional(locations, assembly);
        int pdbIndex = addOptional(locations, pdb);
        int dotnetIndex = addOptional(locations, dotnet);

        return server.getOutputParser().parse(locations).thenApply(parsed -> List.<BuildTargetData>of(
            new DotNetBuildTarget(
                info.getAssemblyName(),
                info.getLanguage(),
                info.getTargetFramework(),
                List.copyOf(parsed.subList(0, sourcesEnd)),
                List.copyOf(parsed.subList(sourcesEnd, frameworkEnd)),
                List.copyOf(parsed.subList(frameworkEnd, packageEnd)),
                List.copyOf(info.getDefinesList()),
                info.getLanguageVersion(),
                info.getNullable(),
                info.getAllowUnsafeBlocks(),
                optional(parsed, assemblyIndex),
                optional(parsed, pdbIndex),
                List.copyOf(info.getInternalsVisibleToList()),
                info.getProjectSdk(),
                info.getExecutable(),
                optional(parsed, dotnetIndex)
            )
        ));
    }

    private static @Nullable ArtifactLocation nonEmpty(ArtifactLocation location) {
        return location.getRelativePath().isEmpty() ? null : location;
    }

    private static int addOptional(List<ArtifactLocation> locations, @Nullable ArtifactLocation location) {
        if (location == null) {
            return -1;
        }
        locations.add(location);
        return locations.size() - 1;
    }

    private static @Nullable OutputLocation optional(List<OutputLocation> parsed, int index) {
        return index < 0 ? null : parsed.get(index);
    }
}
