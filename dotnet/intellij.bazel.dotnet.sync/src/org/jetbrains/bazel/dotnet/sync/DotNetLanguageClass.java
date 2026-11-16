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
package org.jetbrains.bazel.dotnet.sync;

import consulo.annotation.component.ExtensionImpl;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.LanguageClass.LanguageClassProvider;
import org.jetbrains.bazel.commons.TargetKind;

import java.util.List;
import java.util.Set;

/**
 * consulo: Consulo-only, like {@code RustLanguageClass}. Only C# is imported; F# and Visual Basic targets of rules_dotnet are
 * reported by the aspect but not imported (TODO consulo).
 */
public final class DotNetLanguageClass {
    private DotNetLanguageClass() {
    }

    public static final LanguageClass CSHARP = new LanguageClass("csharp", Set.of("cs"));

    public static boolean includesCSharp(TargetKind receiver) {
        return receiver.languageClasses().contains(CSHARP);
    }
}

@ExtensionImpl
class DotNetLanguageClassProvider implements LanguageClassProvider {
    public DotNetLanguageClassProvider() {
    }

    @Override
    public List<LanguageClass> getLanguages() {
        return List.of(DotNetLanguageClass.CSHARP);
    }
}
