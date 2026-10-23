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

package consulo.bazel.languages;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.RGBColor;
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors;

/**
 * Replaces upstream {@code colorSchemes/StarlarkDefault.xml}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class StarlarkDefaultColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public void extend(Builder builder) {
        builder.add(
            StarlarkHighlightingColors.NAMED_ARGUMENT,
            AttributesFlyweightBuilder.create().withForeground(RGBColor.fromRGBValue(0x467CDA)).build()
        );
    }

    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }
}
