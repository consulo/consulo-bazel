/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.aspect.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * consulo: top-level declarations of upstream sdk/lib/aspectchoice.kt. The enums of that file are
 * {@link Rules} and {@link OutputGroups}.
 */
public final class AspectChoice {
    public static final String ASPECT_NAME = "config:aspect.bzl%intellij_aspect";

    private AspectChoice() {
    }

    public static List<Modules> modulesForRules(Iterable<Rules> rules) {
        Set<Rules> rulesets = new HashSet<>();
        for (Rules rule : rules) {
            rulesets.add(rule);
        }

        List<Modules> result = new ArrayList<>();
        for (Modules module : Modules.values()) {
            if (module.getRulesets().isEmpty() || module.getRulesets().stream().anyMatch(rulesets::contains)) {
                result.add(module);
            }
        }
        return result;
    }

    // If the repository names of the rules for certain languages are known, provide the appropriate
    // repo-mapping to be used in the aspect configuration.
    public static Map<String, String> repoMappingForRules(Map<Rules, String> mapping) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Rules, String> entry : mapping.entrySet()) {
            result.put(entry.getKey().getRulesetName(), entry.getValue());
        }
        return result;
    }
}
