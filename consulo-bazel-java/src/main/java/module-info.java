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
import org.jspecify.annotations.NullMarked;

/**
 * Java/Kotlin/Scala integration (upstream java/ modules), an optional library of the Bazel plugin loaded only with
 * consulo.java.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@NullMarked
module consulo.bazel.java {
    requires consulo.bazel.base;

    requires consulo.java.language.api;
    requires consulo.java.language.impl;
}
