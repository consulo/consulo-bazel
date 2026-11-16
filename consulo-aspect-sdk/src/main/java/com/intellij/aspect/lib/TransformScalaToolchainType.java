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

import static com.intellij.aspect.lib.Transformers.SCALA_TOOLCHAIN_CONSTANT;
import static com.intellij.aspect.lib.Transformers.SCALA_TOOLCHAIN_VALUE;

import java.util.List;

// consulo: declared in upstream sdk/lib/transformers.kt
class TransformScalaToolchainType implements Transformer {
    private final String myScalaRepositoryName;

    TransformScalaToolchainType(String scalaRepositoryName) {
        myScalaRepositoryName = scalaRepositoryName;
    }

    public String getScalaRepositoryName() {
        return myScalaRepositoryName;
    }

    @Override
    public void apply(List<LoadStatement> loads, List<String> lines) {
        boolean needsToolchain = lines.removeIf(line -> line.startsWith(SCALA_TOOLCHAIN_CONSTANT + " = "));
        if (needsToolchain) {
            lines.add(0, SCALA_TOOLCHAIN_CONSTANT + " = \"" + myScalaRepositoryName + SCALA_TOOLCHAIN_VALUE + "\"");
        }
    }
}
