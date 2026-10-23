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
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.inject.Inject;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage.BazelQueryFileType;
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage.BazelQueryFlagsFileType;
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage.BazelrcFileType;
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage.BazelVersionFileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;

/**
 * Registers the file types that upstream declares in {@code intellij.bazel.languages.xml}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class BazelLanguagesFileTypeFactory extends FileTypeFactory {
    private final FileNameMatcherFactory myMatcherFactory;

    @Inject
    public BazelLanguagesFileTypeFactory(FileNameMatcherFactory matcherFactory) {
        myMatcherFactory = matcherFactory;
    }

    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(
            StarlarkFileType.INSTANCE,
            myMatcherFactory.createExtensionFileNameMatcher("bzl"),
            myMatcherFactory.createExtensionFileNameMatcher("bazel"),
            myMatcherFactory.createExtensionFileNameMatcher("bzlmod"),
            myMatcherFactory.createExtensionFileNameMatcher("star"),
            myMatcherFactory.createExactFileNameMatcher("BUILD"),
            myMatcherFactory.createExactFileNameMatcher("WORKSPACE"),
            myMatcherFactory.createWildcardFileNameMatcher("*.bzl.template")
        );

        consumer.consume(BazelQueryFileType.INSTANCE, myMatcherFactory.createExtensionFileNameMatcher("bazelquery"));
        consumer.consume(BazelQueryFlagsFileType.INSTANCE, myMatcherFactory.createExtensionFileNameMatcher("bazelqueryflags"));

        consumer.consume(
            BazelVersionFileType.INSTANCE,
            myMatcherFactory.createExactFileNameMatcher(".bazelversion"),
            myMatcherFactory.createExtensionFileNameMatcher("bazelversion")
        );

        consumer.consume(
            BazelrcFileType.INSTANCE,
            myMatcherFactory.createExtensionFileNameMatcher("bazelrc"),
            myMatcherFactory.createExactFileNameMatcher(".bazelrc")
        );
    }
}
