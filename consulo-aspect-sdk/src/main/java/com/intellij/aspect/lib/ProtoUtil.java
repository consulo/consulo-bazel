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

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import java.io.IOException;

/**
 * consulo: the part of upstream private/lib/proto.kt ({@code com.intellij.aspect.private.lib.utils}) used by the SDK.
 * {@code private} is a Java keyword, so it lives in this package. The other declarations of proto.kt are used by the
 * upstream test infrastructure and tools only and are not ported.
 */
final class ProtoUtil {
    private ProtoUtil() {
    }

    /**
     * Parses a text-format protobuf string into {@code type} using reflection.
     * <p>
     * consulo: upstream is an inline function with a reified type parameter; the type is passed explicitly.
     */
    static <T extends Message> T parseTextProto(String text, Class<T> type) throws IOException {
        try {
            Message.Builder builder = (Message.Builder) type.getMethod("newBuilder").invoke(null);
            TextFormat.Parser.newBuilder().build().merge(text, builder);
            return type.cast(builder.build());
        }
        catch (Throwable e) {
            throw new IOException("could not parse text proto " + type, e);
        }
    }
}
