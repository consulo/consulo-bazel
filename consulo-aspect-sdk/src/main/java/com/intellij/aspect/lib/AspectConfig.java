/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.aspect.lib;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * consulo: declared in upstream sdk/lib/config.kt. The secondary constructors stand for the Kotlin default values.
 *
 * @param bazelVersion The Bazel version written into the config file.
 * @param repoMapping  A mapping from default repo names to a specific replacement e.g., canonical repo name.
 * @param useBuiltin   Languages for which to use the builtin rule, i.e., for which to strip rule set loads.
 * @param rulesets     Rule sets for which information will be requested when running the aspect. If null, take
 *                     all Rules mentioned in the repoMapping or useBuiltin.
 * @param attrAspects  Attributes the aspect should walk. If null, take the default of walking all attributes.
 * @param dropNoIde    Whether the aspect should reduce no-ide targets to tomb stones (and, in particular, not provide
 *                     any information for targets only reachable through no-ide targets).
 */
public record AspectConfig(
    String bazelVersion,
    Map<Rules, String> repoMapping,
    Set<Rules> useBuiltin,
    @Nullable Set<Rules> rulesets,
    @Nullable List<String> attrAspects,
    boolean dropNoIde
) {
    public AspectConfig(String bazelVersion, Map<Rules, String> repoMapping, Set<Rules> useBuiltin) {
        this(bazelVersion, repoMapping, useBuiltin, null);
    }

    public AspectConfig(String bazelVersion, Map<Rules, String> repoMapping, Set<Rules> useBuiltin, @Nullable Set<Rules> rulesets) {
        this(bazelVersion, repoMapping, useBuiltin, rulesets, null, false);
    }
}
