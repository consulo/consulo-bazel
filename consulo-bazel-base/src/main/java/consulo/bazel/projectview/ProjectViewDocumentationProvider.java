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
package consulo.bazel.projectview;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.documentation.BazelDocumentationProviderBase;
import consulo.language.Language;
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage;

/**
 * Serves upstream's ProjectView documentation target providers (flags, sections) in {@code .bazelproject} files.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class ProjectViewDocumentationProvider extends BazelDocumentationProviderBase {
    @Override
    public Language getLanguage() {
        return ProjectViewLanguage.INSTANCE;
    }
}
