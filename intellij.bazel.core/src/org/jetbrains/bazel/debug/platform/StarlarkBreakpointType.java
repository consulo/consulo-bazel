package org.jetbrains.bazel.debug.platform;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpointTypeResolver;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionImpl
public class StarlarkBreakpointType extends XLineBreakpointType<StarlarkBreakpointProperties> {
    public StarlarkBreakpointType() {
        super(
            "starlark-line",
            BazelPluginLocalize.starlarkDebugBreakpointTypeTitle().get()
        );
    }

    @Override
    public StarlarkBreakpointProperties createBreakpointProperties(VirtualFile file, int line) {
        return new StarlarkBreakpointProperties();
    }

    // consulo: XLineBreakpointType has no canPutAt(), it is asked by StarlarkBreakpointTypeResolver (below)
    public boolean canPutAt(
        VirtualFile file,
        int line,
        Project project
    ) {
        return supportsStarlarkDebugBreakpoint(file);
    }

    private static boolean supportsStarlarkDebugBreakpoint(VirtualFile receiver) {
        return Objects.equals(receiver.getExtension(), StarlarkFileType.INSTANCE.getDefaultExtension()) ||
            receiver.getName().equals("BUILD") ||
            receiver.getName().equals("BUILD.bazel");
    }
}

/**
 * consulo: Consulo resolves line breakpoint types per file type through {@link XLineBreakpointTypeResolver} instead of
 * {@code XLineBreakpointType.canPutAt}; delegates to {@link StarlarkBreakpointType#canPutAt}.
 */
@ExtensionImpl
class StarlarkBreakpointTypeResolver implements XLineBreakpointTypeResolver {
    public StarlarkBreakpointTypeResolver() {
    }

    @RequiredReadAction
    @Override
    public @Nullable XLineBreakpointType<?> resolveBreakpointType(Project project, VirtualFile virtualFile, int line) {
        StarlarkBreakpointType type =
            Application.get().getExtensionPoint(XBreakpointType.class).findExtensionOrFail(StarlarkBreakpointType.class);
        return type.canPutAt(virtualFile, line, project) ? type : null;
    }

    @Override
    public FileType getFileType() {
        return StarlarkFileType.INSTANCE;
    }
}
