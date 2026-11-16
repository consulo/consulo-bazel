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
 * NUnit support for the C# modules of the Bazel sync (Consulo-only, see CONSULO.md), an optional library of the Bazel plugin
 * loaded only with consulo.nunit and the plugins of consulo.bazel.dotnet.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@NullMarked
module consulo.bazel.dotnet.nunit {
    requires consulo.bazel.base;
    requires consulo.bazel.dotnet;
    requires consulo.nunit.api;

    requires consulo.application.api;
    requires consulo.component.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.ui.api;
    requires consulo.disposer.api;

    exports consulo.bazel.dotnet.nunit.localize;
    exports consulo.bazel.dotnet.nunit.module.extension;
    exports consulo.bazel.dotnet.nunit.workspace;
}
