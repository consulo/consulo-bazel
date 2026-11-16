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
import java.util.Set;

// consulo: declared in upstream sdk/lib/transformers.kt

/**
 * Removes load statements from external repositories not in the allowed list. Used for removing
 * loads when the user's project uses the builtin rules.
 */
class TransformBuiltinRules implements Transformer {
    private final List<String> myRepoNamesToRemove;

    public TransformBuiltinRules(Set<Rules> useBuiltin) {
        myRepoNamesToRemove = useBuiltin.stream().map(Rules::getRulesetName).toList();
    }

    public List<String> getRepoNamesToRemove() {
        return myRepoNamesToRemove;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        loads.removeIf(stmt ->
            stmt.repository() instanceof Repository.External external && myRepoNamesToRemove.contains(external.name())
        );
    }
}
