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

import static com.intellij.aspect.lib.PathUtil.asBazelPath;

import java.nio.file.Path;
import java.util.List;

// consulo: declared in upstream sdk/lib/transformers.kt

/**
 * Rewrites repo-absolute load paths by prepending the deploy directory prefix.
 */
public class TransformRelativePaths implements Transformer {
    private final Path myPrefix;

    public TransformRelativePaths(Path prefix) {
        myPrefix = prefix;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        loads.replaceAll(stmt -> {
            if (stmt.repository() != Repository.Absolute.INSTANCE) {
                return stmt;
            }
            else {
                return new LoadStatement(stmt.repository(), asBazelPath(myPrefix) + "/" + stmt.path(), stmt.arguments());
            }
        });
    }
}
