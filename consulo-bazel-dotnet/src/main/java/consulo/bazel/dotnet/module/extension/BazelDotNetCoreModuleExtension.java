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
package consulo.bazel.dotnet.module.extension;

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.dotnet.compiler.DotNetMacroUtil;
import consulo.dotnet.core.bundle.DotNetCoreBundleType;
import consulo.dotnet.core.msbuild.impl.debug.DotNetCoreDebuggerCommandLine;
import consulo.dotnet.debugger.impl.DotNetDebugProcess;
import consulo.dotnet.debugger.impl.DotNetModuleExtensionWithDebug;
import consulo.dotnet.module.extension.BaseDotNetModuleExtension;
import consulo.dotnet.util.DebugConnectionInfo;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.XDebugSession;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.config.project;
import org.jspecify.annotations.Nullable;

/**
 * The .NET part of a module of a rules_dotnet target: the .NET SDK (a {@link DotNetCoreBundleType} bundle, the hermetic SDK
 * of the target), the target type, the preprocessor symbols and the built assembly (output directory and file name in
 * {@code bazel-out}). Bazel builds the assembly, so the extension does not compile; it runs and debugs the assembly with the
 * {@code dotnet} of the bundle.
 * <p>
 * Like the MSBuild extension of incubating-consulo-dotnet-core ({@code MSBuildDotNetCoreModuleExtension}), without the MSBuild
 * project parts.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelDotNetCoreModuleExtension extends BaseDotNetModuleExtension<BazelDotNetCoreModuleExtension>
    implements DotNetModuleExtensionWithDebug {
    public BazelDotNetCoreModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Override
    public Class<? extends SdkType> getSdkTypeClass() {
        return DotNetCoreBundleType.class;
    }

    @Override
    public boolean isSupportCompilation() {
        // Bazel builds the assembly
        return false;
    }

    @Override
    public DotNetDebugProcess createDebuggerProcess(XDebugSession session, RunProfile runProfile, DebugConnectionInfo info) {
        Sdk sdk = getSdk();
        // the debugger runs on the machine of the bundle
        Platform platform = sdk != null ? sdk.getPlatform() : Platform.current();
        return new BazelDotNetCoreDebugProcess(session, runProfile, info.getHost(), info.getPort(), platform, findWorkspaceRoot());
    }

    private @Nullable VirtualFile findWorkspaceRoot() {
        try {
            return project.getRootDir(getProject());
        }
        catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public GeneralCommandLine createDefaultCommandLine(Sdk sdk, @Nullable DebugConnectionInfo debugConnectionInfo) throws ExecutionException {
        return createCommandLine(sdk, DotNetMacroUtil.expandOutputFile(this), debugConnectionInfo);
    }

    /**
     * Runs {@code assembly} with the {@code dotnet} of {@code sdk}, under netcoredbg when {@code debugConnectionInfo} is given.
     * The Bazel debug runner passes the assembly of the configuration it built, which can differ from the synced one.
     */
    public GeneralCommandLine createCommandLine(Sdk sdk, String assembly, @Nullable DebugConnectionInfo debugConnectionInfo)
        throws ExecutionException {
        return DotNetCoreDebuggerCommandLine.create(sdk, assembly, debugConnectionInfo);
    }

    @Override
    public String getDebugFileExtension() {
        return "pdb";
    }
}
