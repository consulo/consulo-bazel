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
import java.util.Map;
import java.util.Objects;

// consulo: declared in upstream sdk/lib/transformers.kt

/**
 * Remaps external repository names according to the provided mapping.
 */
public class TransformExternalRepositories implements Transformer {
    private final Map<String, String> myNameMapping;

    public TransformExternalRepositories(Map<Rules, String> mapping) {
        myNameMapping = AspectChoice.repoMappingForRules(mapping);
    }

    public Map<String, String> getNameMapping() {
        return myNameMapping;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        loads.replaceAll(stmt -> {
            if (!(stmt.repository() instanceof Repository.External external) || !myNameMapping.containsKey(external.name())) {
                return stmt;
            }
            else {
                return new LoadStatement(
                    new Repository.External(Objects.requireNonNull(myNameMapping.get(external.name()))),
                    stmt.path(),
                    stmt.arguments()
                );
            }
        });
    }
}
