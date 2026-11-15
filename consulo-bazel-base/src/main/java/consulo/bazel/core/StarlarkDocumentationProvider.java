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
package consulo.bazel.core;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.BazelDocumentationProviderBase;
import consulo.language.Language;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;

/**
 * Serves upstream's Starlark (Bazel rules, global functions and their arguments) documentation targets, symbols and links through the classic documentation provider.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class StarlarkDocumentationProvider extends BazelDocumentationProviderBase {
    @Override
    public Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }
}
