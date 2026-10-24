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

import java.nio.file.Path;

/**
 * consulo: the part of upstream private/lib/path.kt ({@code com.intellij.aspect.private.lib.utils}) used by the SDK.
 * {@code private} is a Java keyword, so it lives in this package. The other functions of path.kt are used by the
 * upstream test infrastructure and tools only and are not ported.
 */
final class PathUtil {
    private PathUtil() {
    }

    static String asBazelPath(Path path) {
        String result = path.toString().replace('\\', '/');
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static String joinBazelPath(String... parts) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = i == 0 ? trimEnd(parts[i]) : trimEnd(trimStart(parts[i]));
            if (part.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append('/');
            }
            result.append(part);
        }
        return result.toString();
    }

    // consulo: Kotlin String.trimStart('/') / trimEnd('/')
    private static String trimStart(String value) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return value.substring(start);
    }

    private static String trimEnd(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
