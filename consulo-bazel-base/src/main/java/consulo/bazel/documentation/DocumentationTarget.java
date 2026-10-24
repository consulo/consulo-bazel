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

/**
 * Subset of IntelliJ's {@code com.intellij.platform.backend.documentation.DocumentationTarget} used by upstream code.
 * Consulo has only the classic {@link consulo.language.editor.documentation.DocumentationProvider}; targets are served
 * through {@link BazelDocumentationProviderBase}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public interface DocumentationTarget {
    /**
     * Short text shown on hover or in quick navigation. Same contract as upstream.
     */
    default @Nullable String computeDocumentationHint() {
        return null;
    }

    @Nullable DocumentationResult computeDocumentation();
}
