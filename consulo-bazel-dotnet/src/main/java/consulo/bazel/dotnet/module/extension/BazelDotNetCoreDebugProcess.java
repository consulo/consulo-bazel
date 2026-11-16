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

import consulo.dotnet.core.msbuild.impl.debug.DotNetCoreDebugProcess;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.XDebugSession;
import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * netcoredbg of a rules_dotnet assembly. rules_dotnet passes the sources to the compiler by paths relative to the execution root,
 * so the debug symbols know a workspace source as {@code cs/greeter/Greeter.cs}. netcoredbg matches a relative breakpoint path
 * against the end of the symbol paths, but an absolute one only exactly, and reports frames by the relative symbol path: a source
 * of the workspace is sent by its path relative to the workspace root, and a relative path of a frame is resolved against it.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public class BazelDotNetCoreDebugProcess extends DotNetCoreDebugProcess {
    private final @Nullable VirtualFile myWorkspaceRoot;

    public BazelDotNetCoreDebugProcess(
        XDebugSession session,
        RunProfile runProfile,
        String host,
        int port,
        Platform platform,
        @Nullable VirtualFile workspaceRoot
    ) {
        super(session, runProfile, host, port, platform);
        myWorkspaceRoot = workspaceRoot;
    }

    @Override
    protected String toSourcePath(String filePath) {
        VirtualFile root = myWorkspaceRoot;
        if (root == null) {
            return filePath;
        }
        String relative = FileUtil.getRelativePath(root.getPath(), filePath, '/');
        if (relative == null || relative.startsWith("..")) {
            return filePath;
        }
        return relative;
    }

    @Override
    protected @Nullable VirtualFile findSourceFile(String path) {
        VirtualFile root = myWorkspaceRoot;
        String systemIndependent = FileUtil.toSystemIndependentName(path);
        if (root != null && !FileUtil.isAbsolute(systemIndependent)) {
            VirtualFile file = root.findFileByRelativePath(systemIndependent);
            if (file != null) {
                return file;
            }
        }
        return super.findSourceFile(path);
    }
}
