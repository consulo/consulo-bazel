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
package consulo.bazel.cpp.workspace;

import consulo.cpp.moduleAware.CFileOptions;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The compiler switches of one language of a C/C++ resolve configuration.
 * <p>
 * Consulo-only: stands in for CIDR's {@code CidrCompilerSwitches} and {@code CompilerSpecificSwitchBuilder}, which the
 * upstream workspace import writes. consulo-cpp has no compiler switches: a file is preprocessed with defines and
 * include directories ({@link CFileOptions}), so the builder keeps those apart from the raw switches.
 * <p>
 * TODO consulo: consulo-cpp has no compiler switch parser nor compiler info collection (CIDR {@code CompilerInfoCache}
 * runs the compiler with the switches): the {@code -D}, {@code -U}, {@code -include} and {@code -std} options among the
 * raw switches (the toolchain options and the copts that are not paths) and the sysroot do not reach the file options.
 *
 * @param raw                the switches as the compiler gets them, in GNU spelling
 * @param macros             the macros passed with {@code withMacro}, a value is empty for {@code -DNAME}
 * @param includePaths       the {@code -I} directories
 * @param quoteIncludePaths  the {@code -iquote} directories
 * @param systemIncludePaths the {@code -isystem} directories
 * @param sysroot            the {@code --sysroot}
 * @author VISTALL
 * @since 2026-11-16
 */
public record CcCompilerSwitches(
    List<String> raw,
    Map<String, String> macros,
    List<String> includePaths,
    List<String> quoteIncludePaths,
    List<String> systemIncludePaths,
    @Nullable String sysroot
) {
    public static final CcCompilerSwitches EMPTY = new CcCompilerSwitches(List.of(), Map.of(), List.of(), List.of(), List.of(), null);

    public CcCompilerSwitches {
        raw = List.copyOf(raw);
        // keeps the order of the defines
        macros = Collections.unmodifiableMap(new LinkedHashMap<>(macros));
        includePaths = List.copyOf(includePaths);
        quoteIncludePaths = List.copyOf(quoteIncludePaths);
        systemIncludePaths = List.copyOf(systemIncludePaths);
    }

    /**
     * The options consulo-cpp preprocesses a file with. consulo-cpp has one list of include directories: the quote
     * directories come first, then the {@code -I} and the system ones, the order the compiler searches a
     * {@code #include "..."} in.
     */
    public CFileOptions toFileOptions() {
        List<String> paths = new ArrayList<>(quoteIncludePaths);
        paths.addAll(includePaths);
        paths.addAll(systemIncludePaths);
        // TODO consulo: the language standard (-std among the raw switches) is not known, see the class comment
        return new CFileOptions(macros, paths, List.of(), null);
    }

    /**
     * consulo: CIDR {@code CompilerSpecificSwitchBuilder.getBuilder(kind)}.
     * <p>
     * TODO consulo: CIDR has a builder per compiler kind (GNU or MSVC spelling); consulo-cpp has no compiler kinds,
     * the raw switches always use the GNU spelling
     */
    public static Builder getBuilder(CcCompilerKind kind) {
        return new Builder(kind);
    }

    /**
     * consulo: CIDR {@code CompilerSpecificSwitchBuilder}, with the same methods.
     */
    public static final class Builder {
        private final CcCompilerKind myKind;
        private final List<String> myRaw = new ArrayList<>();
        private final Map<String, String> myMacros = new LinkedHashMap<>();
        private final List<String> myIncludePaths = new ArrayList<>();
        private final List<String> myQuoteIncludePaths = new ArrayList<>();
        private final List<String> mySystemIncludePaths = new ArrayList<>();
        private @Nullable String mySysroot;

        private Builder(CcCompilerKind kind) {
            myKind = kind;
        }

        public CcCompilerKind getKind() {
            return myKind;
        }

        public Builder withSwitch(String value) {
            myRaw.add(value);
            return this;
        }

        /**
         * A Bazel define: {@code NAME} or {@code NAME=VALUE}.
         */
        public Builder withMacro(String define) {
            myRaw.add("-D" + define);
            int eq = define.indexOf('=');
            if (eq < 0) {
                myMacros.put(define, "");
            }
            else {
                myMacros.put(define.substring(0, eq), define.substring(eq + 1));
            }
            return this;
        }

        public Builder withIncludePath(String path) {
            myRaw.add("-I" + path);
            myIncludePaths.add(path);
            return this;
        }

        public Builder withQuoteIncludePath(String path) {
            myRaw.add("-iquote");
            myRaw.add(path);
            myQuoteIncludePaths.add(path);
            return this;
        }

        public Builder withSystemIncludePath(String path) {
            myRaw.add("-isystem");
            myRaw.add(path);
            mySystemIncludePaths.add(path);
            return this;
        }

        public Builder withSysroot(String path) {
            myRaw.add("--sysroot=" + path);
            mySysroot = path;
            return this;
        }

        /**
         * The switches as the compiler gets them.
         */
        public List<String> buildRaw() {
            return List.copyOf(myRaw);
        }

        public CcCompilerSwitches build() {
            return new CcCompilerSwitches(myRaw, myMacros, myIncludePaths, myQuoteIncludePaths, mySystemIncludePaths, mySysroot);
        }
    }
}
