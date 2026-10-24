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
 * Java port of the IntelliJ aspect SDK (JetBrains/intellij-aspect, see CONSULO.md):
 * <ul>
 * <li>generated protobuf classes of {@code sdk/proto}. The classes are generated with the full protobuf API
 * (not lite): the plugin parses the aspect text output;</li>
 * <li>{@code com.intellij.aspect.lib}: {@code sdk/lib/*.kt} converted to Java (deploy and read the aspect);</li>
 * <li>{@code archive_ide.zip} at the module root: the aspect Starlark sources ({@code src/main/aspect}).</li>
 * </ul>
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module consulo.bazel.aspect.sdk {
    requires transitive com.google.protobuf;
    requires static org.jspecify;

    exports com.intellij.aspect.lib;

    exports com.google.devtools.intellij.aspect;
    exports com.google.devtools.intellij.ideinfo;
}
