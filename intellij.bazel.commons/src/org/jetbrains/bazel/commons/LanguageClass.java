/*
 * Copyright 2016-2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.commons;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Supported languages/technologies
 * <p>
 * NOTE: serialId is being used in serialization - keep it unchanged
 */
public record LanguageClass(
    String languageName,
    Set<String> recognizedFilenameExtensions,

    // When collection FUS hide the overridden language to not to duplicate statistics
    // TODO: develop more elegant solution
    @Nullable LanguageClass fusOverride
) {
    public LanguageClass(String languageName, Set<String> recognizedFilenameExtensions) {
        this(languageName, recognizedFilenameExtensions, null);
    }

    @Override
    public String toString() {
        return languageName;
    }

    /** Returns the LanguageClass associated with the given filename extension, if it's recognized.  */
    public static @Nullable LanguageClass fromExtension(String filenameExtension) {
        return LanguageClassService.getInstance().fromExtension(filenameExtension);
    }

    /**
     * Provides a set of recognized bazel language classes.
     * Individual language-specific sub-plugins can use
     * this EP to register new language class
     */
    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface LanguageClassProvider {
        List<LanguageClass> getLanguages();

        static ExtensionPoint<LanguageClassProvider> getEp() {
            return Application.get().getExtensionPoint(LanguageClassProvider.class);
        }
    }
}
