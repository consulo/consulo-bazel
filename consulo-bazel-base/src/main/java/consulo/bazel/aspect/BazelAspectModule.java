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
package consulo.bazel.aspect;

import com.intellij.aspect.lib.AspectChoice;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Published Bazel modules of the IntelliJ aspect: ours ({@code consulo_aspect}, from
 * <a href="https://github.com/consulo/bazel-aspects">consulo/bazel-aspects</a>) and JetBrains' ({@code intellij_aspect}).
 * <p>
 * When the root module of the workspace has a {@code bazel_dep} on one of them, sync uses the aspect of that module
 * ({@code @@<canonical>//config:aspect.bzl%intellij_aspect}) and does not materialize the aspect into the workspace.
 * Both write the same {@code ide_info.proto} textprotos; {@code consulo_aspect} is preferred when both are present.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public enum BazelAspectModule {
    CONSULO("consulo_aspect"),
    INTELLIJ("intellij_aspect");

    /**
     * An external aspect module resolved in the workspace.
     *
     * @param canonicalRepoName canonical name of the module's repository, e.g. {@code consulo_aspect+}
     */
    public record Resolved(BazelAspectModule module, String canonicalRepoName) {
        /**
         * Label of the aspect for {@code --aspects}; {@code aspect} is relative to the module root
         * (e.g. {@link AspectChoice#ASPECT_NAME}).
         */
        public String aspectLabel(String aspect) {
            return "@@" + canonicalRepoName + "//" + aspect;
        }
    }

    private final String myModuleName;

    BazelAspectModule(String moduleName) {
        myModuleName = moduleName;
    }

    public String getModuleName() {
        return myModuleName;
    }

    /**
     * Finds a {@code bazel_dep} of the root module on one of the aspect modules. Only Bzlmod workspaces can depend
     * on a module; for WORKSPACE projects the result is always {@code null} (the aspect is materialized).
     */
    public static @Nullable Resolved detect(RepoMapping repoMapping) {
        if (!(repoMapping instanceof RepoMapping.BzlmodRepoMapping bzlmodRepoMapping)) {
            return null;
        }
        Map<String, String> apparentToCanonical = bzlmodRepoMapping.getApparentRepoNameToCanonicalName();
        for (BazelAspectModule module : values()) {
            for (String canonicalName : apparentToCanonical.values()) {
                if (isRepositoryOf(canonicalName, module.myModuleName)) {
                    return new Resolved(module, canonicalName);
                }
            }
        }
        return null;
    }

    /**
     * Canonical repository names of Bazel modules are {@code <module>+} (Bazel 8+) or {@code <module>~[version]} (Bazel 7).
     */
    private static boolean isRepositoryOf(String canonicalName, String moduleName) {
        if (!canonicalName.startsWith(moduleName) || canonicalName.length() == moduleName.length()) {
            return false;
        }
        char separator = canonicalName.charAt(moduleName.length());
        return separator == '+' || separator == '~';
    }
}
