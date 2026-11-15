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

import consulo.annotation.component.ExtensionImpl;
import consulo.cpp.moduleAware.CFileOptions;
import consulo.cpp.moduleAware.CFileOptionsContributor;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.clion.workspace.CcDirectoryBasedChooser;
import org.jetbrains.bazel.clion.workspace.buildWorkspaceModel;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Gives a C/C++ file of a Bazel project the defines and include directories of its resolve configuration.
 * <p>
 * Consulo-only: in CLion the OC workspace the import writes ({@link CcWorkspaceModel}) is what the editor resolves a
 * file with; consulo-cpp asks the {@code CFileOptionsContributor}s.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class CcFileOptionsContributor implements CFileOptionsContributor {
    @Override
    public @Nullable CFileOptions getOptions(Module module, VirtualFile file) {
        Project project = module.getProject();
        CcWorkspaceModel workspace = CcWorkspaceModel.getInstance(project);
        if (workspace == null) {
            return null;
        }

        // TODO consulo: CLion lets resolve configuration choosers pick one of the configurations of a file (upstream
        //  CcRunConfigurationChooser of clion.core, the one of the selected run configuration); consulo-cpp gives a file
        //  one set of options, the first configuration is used
        List<CcWorkspaceModel.Configuration> configurations = workspace.getConfigurationsForFile(file);
        for (CcWorkspaceModel.Configuration configuration : configurations) {
            CcWorkspaceModel.CompilerSettings settings = configuration.getSourceCompilerSettings(file.getUrl());
            if (settings != null) {
                return settings.getFileOptions();
            }
        }

        // not a source of any configuration, e.g. a header or a new file
        CcWorkspaceModel.Configuration configuration = new CcDirectoryBasedChooser().selectPreselectedResolveConfiguration(project, file);
        if (configuration == null) {
            return null;
        }
        CcWorkspaceModel.CompilerSettings settings =
            configuration.findLanguageCompilerSettings(buildWorkspaceModel.getDeclaredLanguageKind(file.getName()));
        return settings == null ? null : settings.getFileOptions();
    }
}
