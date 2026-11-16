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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The preprocessor symbols rules_dotnet passes to the compiler besides the {@code defines} of a target.
 * <p>
 * consulo: Consulo-only, a port of rules_dotnet's {@code framework_preprocessor_symbols} and {@code FRAMEWORK_COMPATIBILITY}
 * ({@code dotnet/private/common.bzl}) and of the {@code /define:TRACE} / {@code /define:DEBUG} of {@code csharp_assembly.bzl}.
 */
public final class DotNetFrameworkDefines {
    // the target frameworks a framework can compile against (transitive), in the order of rules_dotnet
    private static final Map<String, List<String>> FRAMEWORK_COMPATIBILITY = new LinkedHashMap<>();

    static {
        // .NET Standard
        compatibility("netstandard");
        compatibility("netstandard1.0", "netstandard");
        compatibility("netstandard1.1", "netstandard1.0");
        compatibility("netstandard1.2", "netstandard1.1");
        compatibility("netstandard1.3", "netstandard1.2");
        compatibility("netstandard1.4", "netstandard1.3");
        compatibility("netstandard1.5", "netstandard1.4");
        compatibility("netstandard1.6", "netstandard1.5");
        compatibility("netstandard2.0", "netstandard1.6");
        compatibility("netstandard2.1", "netstandard2.0");

        // .NET Framework
        compatibility("net11");
        compatibility("net20", "net11");
        compatibility("net30", "net20");
        compatibility("net35", "net30");
        compatibility("net40", "net35");
        compatibility("net403", "net40");
        compatibility("net45", "net403", "netstandard1.1");
        compatibility("net451", "net45", "netstandard1.2");
        compatibility("net452", "net451");
        compatibility("net46", "net452", "netstandard1.3");
        compatibility("net461", "net46", "netstandard2.0");
        compatibility("net462", "net461");
        compatibility("net47", "net462");
        compatibility("net471", "net47");
        compatibility("net472", "net471");
        compatibility("net48", "net472");
        compatibility("net481", "net48");

        // .NET Core
        compatibility("netcoreapp1.0", "netstandard1.6");
        compatibility("netcoreapp1.1", "netcoreapp1.0");
        compatibility("netcoreapp2.0", "netcoreapp1.1", "netstandard2.0");
        compatibility("netcoreapp2.1", "netcoreapp2.0");
        compatibility("netcoreapp2.2", "netcoreapp2.1");
        compatibility("netcoreapp3.0", "netcoreapp2.2", "netstandard2.1");
        compatibility("netcoreapp3.1", "netcoreapp3.0");
        compatibility("net5.0", "netcoreapp3.1");
        compatibility("net6.0", "net5.0");
        compatibility("net7.0", "net6.0");
        compatibility("net8.0", "net7.0");
        compatibility("net9.0", "net8.0");
        compatibility("net10.0", "net9.0");
    }

    private DotNetFrameworkDefines() {
    }

    private static void compatibility(String framework, String... compatible) {
        FRAMEWORK_COMPATIBILITY.put(framework, List.of(compatible));
    }

    /**
     * rules_dotnet's {@code framework_preprocessor_symbols(tfm)}: {@code NET8_0}, the {@code *_OR_GREATER} symbol of every
     * compatible framework and the symbol of the framework family. An unknown framework (newer than the table) gets its own
     * symbols only.
     */
    public static List<String> frameworkPreprocessorSymbols(String tfm) {
        List<String> defines = new ArrayList<>();
        if (tfm.isEmpty()) {
            return defines;
        }
        defines.add(toSymbol(tfm));
        for (String framework : transitiveCompatibility(tfm)) {
            defines.add(toSymbol(framework) + "_OR_GREATER");
        }

        if (tfm.startsWith("netstandard")) {
            defines.add("NETSTANDARD");
        }
        else if (tfm.startsWith("netcoreapp")) {
            defines.add("NETCOREAPP");
        }
        else if (tfm.startsWith("net4")) {
            defines.add("NETFRAMEWORK");
        }
        else if (tfm.startsWith("net")) {
            defines.add("NET");
        }
        return defines;
    }

    /**
     * All preprocessor symbols of a target compiled by rules_dotnet: {@code TRACE} and {@code DEBUG} (the compilation modes
     * {@code fastbuild} and {@code dbg}), the framework symbols and the {@code defines} of the target.
     * <p>
     * TODO consulo: the compilation mode of the sync is not known here, {@code -c opt} defines {@code RELEASE} instead of
     * {@code DEBUG}.
     */
    public static List<String> compilerDefines(DotNetBuildTarget target) {
        Set<String> result = new LinkedHashSet<>();
        result.add("TRACE");
        result.add("DEBUG");
        result.addAll(frameworkPreprocessorSymbols(target.targetFramework()));
        result.addAll(target.defines());
        return new ArrayList<>(result);
    }

    // the transitive closure, the framework itself included (rules_dotnet _collect_transitive)
    private static Set<String> transitiveCompatibility(String tfm) {
        Set<String> result = new LinkedHashSet<>();
        List<String> queue = new ArrayList<>();
        queue.add(tfm);
        while (!queue.isEmpty()) {
            String framework = queue.remove(queue.size() - 1);
            if (!result.add(framework)) {
                continue;
            }
            queue.addAll(FRAMEWORK_COMPATIBILITY.getOrDefault(framework, List.of()));
        }
        return result;
    }

    // net8.0 -> NET8_0, net461 -> NET461
    private static String toSymbol(String framework) {
        return framework.toUpperCase(Locale.ROOT).replace(".", "_");
    }
}
