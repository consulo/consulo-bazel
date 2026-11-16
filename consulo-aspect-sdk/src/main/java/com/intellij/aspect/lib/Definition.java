/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.aspect.lib;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * consulo: top-level declarations of upstream sdk/lib/definition.kt. The class of that file is {@link AspectDefinition}.
 * <p>
 * Upstream initializes the definitions with {@code by lazy}; here they are read when this class is initialized,
 * a missing or broken bundled resource is reported as an {@link UncheckedIOException}.
 */
public final class Definition {
    /**
     * The default aspect run during the sync.
     */
    public static final AspectDefinition INTELLIJ_ASPECT = fromResource("/config/aspect_ide.textproto");

    /**
     * The debug aspect for CC targets. Reports the compile and link command lines and collects the
     * files the debugger reads in the {@code intellij-debug} output group.
     */
    public static final AspectDefinition INTELLIJ_CC_DEBUG_ASPECT = fromResource("/config/cc_debug_aspect.textproto");

    private Definition() {
    }

    private static AspectDefinition fromResource(String name) {
        try {
            return AspectDefinition.fromResource(name);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
