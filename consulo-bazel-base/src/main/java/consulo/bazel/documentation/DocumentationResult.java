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
package consulo.bazel.documentation;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Subset of IntelliJ's {@code com.intellij.platform.backend.documentation.DocumentationResult}.
 * Asynchronous documentation is computed when requested: the classic Consulo documentation provider already runs
 * outside the UI thread.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public sealed interface DocumentationResult permits DocumentationResult.Documentation, DocumentationResult.AsyncDocumentation {
    static Documentation documentation(String html) {
        return new Documentation(html);
    }

    static DocumentationResult asyncDocumentation(Supplier<? extends @Nullable DocumentationResult> supplier) {
        return new AsyncDocumentation(supplier);
    }

    record Documentation(String html) implements DocumentationResult {
    }

    record AsyncDocumentation(Supplier<? extends @Nullable DocumentationResult> supplier) implements DocumentationResult {
    }

    /**
     * Resolves the result to the documentation HTML, computing asynchronous results.
     */
    static @Nullable String toHtml(@Nullable DocumentationResult result) {
        DocumentationResult current = result;
        while (current instanceof AsyncDocumentation async) {
            current = async.supplier().get();
        }
        return current instanceof Documentation documentation ? documentation.html() : null;
    }
}
