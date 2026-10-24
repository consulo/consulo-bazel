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

/**
 * consulo: private top-level functions of upstream sdk/lib/transformers.kt. The transformer classes of that file
 * are {@link TransformRelativePaths}, {@link TransformExternalRepositories}, {@link TransformBuiltinRules},
 * {@link TransformFieldLoad}, {@link TransformCcToolchainType}, {@link TransformPythonToolchainType},
 * {@link TransformJavaSemantics} and {@link TransformScalaToolchainType}.
 */
final class Transformers {
    private Transformers() {
    }

    static String label(String value) {
        return "Label(\"" + value + "\")";
    }
}
