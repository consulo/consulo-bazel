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
 * Upstream Bazel plugin core modules compiled as one module (they share packages), see CONSULO.md:
 * {@code intellij.bazel.languages}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module consulo.bazel.base {
    requires transitive consulo.language.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.language.code.style.api;
    requires consulo.color.scheme.api;
    requires consulo.code.editor.api;
    requires consulo.ui.api;

    // generated
    exports consulo.bazel.languages.icon;
    exports consulo.bazel.languages.localize;

    // consulo-only
    exports consulo.bazel.languages;

    exports org.jetbrains.bazel.languages;
    exports org.jetbrains.bazel.languages.bazelquery;
    exports org.jetbrains.bazel.languages.bazelquery.elements;
    exports org.jetbrains.bazel.languages.bazelquery.highlighting;
    exports org.jetbrains.bazel.languages.bazelquery.lexer;
    exports org.jetbrains.bazel.languages.bazelquery.parser;
    exports org.jetbrains.bazel.languages.bazelquery.psi;
    exports org.jetbrains.bazel.languages.bazelrc;
    exports org.jetbrains.bazel.languages.bazelrc.commenter;
    exports org.jetbrains.bazel.languages.bazelrc.elements;
    exports org.jetbrains.bazel.languages.bazelrc.highlighting;
    exports org.jetbrains.bazel.languages.bazelrc.lexer;
    exports org.jetbrains.bazel.languages.bazelrc.matching;
    exports org.jetbrains.bazel.languages.bazelrc.parser;
    exports org.jetbrains.bazel.languages.bazelrc.psi;
    exports org.jetbrains.bazel.languages.bazelversion;
    exports org.jetbrains.bazel.languages.bazelversion.parser;
    exports org.jetbrains.bazel.languages.bazelversion.psi;
    exports org.jetbrains.bazel.languages.starlark;
    exports org.jetbrains.bazel.languages.starlark.bazel;
    exports org.jetbrains.bazel.languages.starlark.commenter;
    exports org.jetbrains.bazel.languages.starlark.elements;
    exports org.jetbrains.bazel.languages.starlark.folding;
    exports org.jetbrains.bazel.languages.starlark.highlighting;
    exports org.jetbrains.bazel.languages.starlark.indentation;
    exports org.jetbrains.bazel.languages.starlark.lexer;
    exports org.jetbrains.bazel.languages.starlark.matching;
    exports org.jetbrains.bazel.languages.starlark.parser;
    exports org.jetbrains.bazel.languages.starlark.psi;
    exports org.jetbrains.bazel.languages.starlark.psi.expressions;
    exports org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;
    exports org.jetbrains.bazel.languages.starlark.psi.functions;
    exports org.jetbrains.bazel.languages.starlark.psi.statements;
    exports org.jetbrains.bazel.languages.starlark.references;
    exports org.jetbrains.bazel.languages.starlark.rename;
    exports org.jetbrains.bazel.languages.starlark.utils;
}
