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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// consulo: declared in upstream sdk/lib/transformers.kt

/**
 * Rewrites the repository of all top-level `PLATFORM_*` label constants for the platform module.
 */
class TransformPlatformsRepository implements Transformer {
    private static final Pattern REGEX = Pattern.compile("^PLATFORM_\\w+ = \"(@platforms)//[^\"]+\"");

    private final String myRepositoryName;

    TransformPlatformsRepository(String repositoryName) {
        myRepositoryName = repositoryName;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        lines.replaceAll(line -> {
            Matcher matcher = REGEX.matcher(line);
            if (!matcher.find()) {
                return line;
            }
            return line.substring(0, matcher.start(1)) + myRepositoryName + line.substring(matcher.end(1));
        });
    }
}
