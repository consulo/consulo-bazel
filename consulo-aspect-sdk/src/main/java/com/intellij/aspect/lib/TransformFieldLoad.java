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

// consulo: declared in upstream sdk/lib/transformers.kt

/**
 * Replaces a loaded field with a top-level variable for builtin deployment.
 */
abstract class TransformFieldLoad implements Transformer {
    private final String myFieldName;
    private final String myReplacement;

    protected TransformFieldLoad(String fieldName, String replacement) {
        myFieldName = fieldName;
        myReplacement = replacement;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        boolean needsField = loads.removeIf(stmt ->
            stmt.repository() instanceof Repository.External && stmt.arguments().contains(myFieldName)
        );

        if (needsField) {
            lines.add(0, myFieldName + " = " + myReplacement);
        }
    }
}
