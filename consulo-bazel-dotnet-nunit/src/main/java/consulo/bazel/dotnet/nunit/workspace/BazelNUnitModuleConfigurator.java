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
package consulo.bazel.dotnet.nunit.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.dotnet.nunit.module.extension.BazelNUnitMutableModuleExtension;
import consulo.bazel.dotnet.workspace.BazelDotNetModuleConfigurator;
import consulo.module.content.layer.ModifiableRootModel;
import org.jetbrains.bazel.dotnet.sync.DotNetBuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;

import java.nio.file.Path;

/**
 * Enables {@code bazel-nunit} for a C# target that references {@code nunit.framework.dll} (a {@code csharp_nunit_test}, or any
 * target with the NUnit NuGet package as a direct dependency), and disables it otherwise.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class BazelNUnitModuleConfigurator implements BazelDotNetModuleConfigurator {
    private static final String NUNIT_FRAMEWORK = "nunit.framework.dll";

    @Override
    public void configure(ModifiableRootModel model, BuildTarget target, DotNetBuildTarget dotNetTarget) {
        BazelNUnitMutableModuleExtension extension = model.getExtensionWithoutCheck(BazelNUnitMutableModuleExtension.class);
        if (extension != null) {
            extension.setEnabled(referencesNUnit(dotNetTarget));
        }
    }

    private static boolean referencesNUnit(DotNetBuildTarget target) {
        for (OutputLocation reference : target.packageReferences()) {
            Path fileName = OutputLocation.getRelativeNioPath(reference).getFileName();
            if (fileName != null && NUNIT_FRAMEWORK.equalsIgnoreCase(fileName.toString())) {
                return true;
            }
        }
        return false;
    }
}
