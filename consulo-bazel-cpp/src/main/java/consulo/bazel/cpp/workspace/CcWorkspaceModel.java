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

import consulo.bazel.workspace.BazelProjectModelService;
import consulo.cpp.moduleAware.CFileOptions;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The C/C++ resolve configurations of a Bazel project: which compiler, defines and include directories every source
 * file is compiled with.
 * <p>
 * Consulo-only: the counterpart of CLion's OC workspace ({@code OCWorkspace}, its {@code ModifiableModel} and the
 * {@code OCResolveConfiguration}s), which the upstream workspace import writes. consulo-cpp keeps the options of a file
 * in its module ({@code CModuleExtension}) and lets {@code CFileOptionsContributor}s add to them per file; this model is
 * published as project data of the {@code BazelModelBuilder} and {@link CcFileOptionsContributor} gives each file the
 * options of its configuration.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public final class CcWorkspaceModel {
    /**
     * The model of the last sync, {@code null} before the first sync or when the C/C++ import did not run.
     */
    public static @Nullable CcWorkspaceModel getInstance(Project project) {
        return BazelProjectModelService.getInstance(project).getProjectData(CcWorkspaceModel.class);
    }

    private final String myClientKey;
    private final Map<String, Configuration> myConfigurations = new LinkedHashMap<>();
    private final Map<String, List<Configuration>> myFileConfigurations = new HashMap<>();

    /**
     * @param clientKey upstream {@code OCWorkspaceImpl.getModifiableModel(clientKey, clear)}
     */
    public CcWorkspaceModel(String clientKey) {
        myClientKey = clientKey;
    }

    public String getClientKey() {
        return myClientKey;
    }

    /**
     * upstream {@code OCWorkspace.ModifiableModel.addConfiguration(id, name, variant)}
     */
    public Configuration addConfiguration(String id, String name) {
        Configuration configuration = new Configuration(id, name);
        myConfigurations.put(id, configuration);
        return configuration;
    }

    public @Nullable Configuration getConfigurationById(String id) {
        return myConfigurations.get(id);
    }

    public Collection<Configuration> getConfigurations() {
        return Collections.unmodifiableCollection(myConfigurations.values());
    }

    /**
     * The configurations {@code file} is a source of.
     */
    public List<Configuration> getConfigurationsForFile(VirtualFile file) {
        return getConfigurationsForFile(file.getUrl());
    }

    public List<Configuration> getConfigurationsForFile(String url) {
        List<Configuration> configurations = myFileConfigurations.get(url);
        return configurations == null ? List.of() : Collections.unmodifiableList(configurations);
    }

    /**
     * A resolve configuration: upstream {@code OCResolveConfiguration} / {@code OCResolveConfiguration.ModifiableModel}.
     */
    public final class Configuration {
        private final String myId;
        private final String myName;
        private final Map<String, CompilerSettings> mySources = new LinkedHashMap<>();
        private final Map<FileType, CompilerSettings> myLanguageSettings = new LinkedHashMap<>();

        private Configuration(String id, String name) {
            myId = id;
            myName = name;
        }

        /**
         * upstream {@code OCResolveConfiguration.getUniqueId()}
         */
        public String getUniqueId() {
            return myId;
        }

        public String getName() {
            return myName;
        }

        /**
         * upstream {@code addSource(url, languageKind)}; the language kind is the consulo-cpp file type of a C or C++
         * source ({@code CSourceFileType}, {@code CPPSourceFileType}).
         */
        public CompilerSettings addSource(String url, FileType languageKind) {
            CompilerSettings settings = new CompilerSettings(languageKind);
            mySources.put(url, settings);
            myFileConfigurations.computeIfAbsent(url, it -> new ArrayList<>(1)).add(this);
            return settings;
        }

        public Map<String, CompilerSettings> getSources() {
            return Collections.unmodifiableMap(mySources);
        }

        public @Nullable CompilerSettings getSourceCompilerSettings(String url) {
            return mySources.get(url);
        }

        /**
         * The settings of the files of a language that are not sources of the configuration, e.g. headers.
         */
        public CompilerSettings getLanguageCompilerSettings(FileType languageKind) {
            return myLanguageSettings.computeIfAbsent(languageKind, CompilerSettings::new);
        }

        /**
         * Reads the language settings of a published model without adding them.
         */
        public @Nullable CompilerSettings findLanguageCompilerSettings(FileType languageKind) {
            return myLanguageSettings.get(languageKind);
        }

        @Override
        public String toString() {
            return "Configuration(" + myId + ", " + myName + ")";
        }
    }

    /**
     * upstream {@code OCCompilerSettings.ModifiableModel}
     */
    public static final class CompilerSettings {
        private final FileType myLanguageKind;
        private CcCompilerKind myCompilerKind = CcCompilerKind.UNKNOWN;
        private @Nullable Path myCompiler;
        private @Nullable Path myCompilerWorkingDir;
        private CcCompilerSwitches mySwitches = CcCompilerSwitches.EMPTY;
        private CFileOptions myFileOptions = CcCompilerSwitches.EMPTY.toFileOptions();

        private CompilerSettings(FileType languageKind) {
            myLanguageKind = languageKind;
        }

        public FileType getLanguageKind() {
            return myLanguageKind;
        }

        // consulo: upstream passes java.io.File
        public void setCompiler(CcCompilerKind kind, Path compiler, Path compilerWorkingDir) {
            myCompilerKind = kind;
            myCompiler = compiler;
            myCompilerWorkingDir = compilerWorkingDir;
        }

        public void setCompilerSwitches(CcCompilerSwitches switches) {
            mySwitches = switches;
            myFileOptions = switches.toFileOptions();
        }

        public CcCompilerKind getCompilerKind() {
            return myCompilerKind;
        }

        public @Nullable Path getCompiler() {
            return myCompiler;
        }

        public @Nullable Path getCompilerWorkingDir() {
            return myCompilerWorkingDir;
        }

        public CcCompilerSwitches getCompilerSwitches() {
            return mySwitches;
        }

        /**
         * What consulo-cpp preprocesses the files of these settings with.
         */
        public CFileOptions getFileOptions() {
            return myFileOptions;
        }
    }
}
