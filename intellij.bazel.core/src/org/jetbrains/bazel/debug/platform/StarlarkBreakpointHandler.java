package org.jetbrains.bazel.debug.platform;

import consulo.execution.debug.breakpoint.XBreakpointHandler;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

// consulo: upstream typealias StarlarkBreakpoint = XLineBreakpoint<StarlarkBreakpointProperties>
public class StarlarkBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<StarlarkBreakpointProperties>> {
    private final StarlarkDebugMessenger myMessenger;
    private final Set<XLineBreakpoint<StarlarkBreakpointProperties>> myCurrent = new LinkedHashSet<>();

    public StarlarkBreakpointHandler(StarlarkDebugMessenger messenger) {
        super(StarlarkBreakpointType.class);
        myMessenger = messenger;
    }

    @Override
    public void registerBreakpoint(XLineBreakpoint<StarlarkBreakpointProperties> breakpoint) {
        synchronized (this) {
            myCurrent.add(breakpoint);
            flushBreakpoints();
        }
    }

    @Override
    public void unregisterBreakpoint(XLineBreakpoint<StarlarkBreakpointProperties> breakpoint, boolean temporary) {
        synchronized (this) {
            myCurrent.remove(breakpoint);
            flushBreakpoints();
        }
    }

    private void flushBreakpoints() {
        myMessenger.setBreakpoints(new ArrayList<>(myCurrent));
    }

    public @Nullable XLineBreakpoint<StarlarkBreakpointProperties> getBreakpointByPathAndLine(String path, int line) {
        for (XLineBreakpoint<StarlarkBreakpointProperties> it : myCurrent) {
            if (absolutePath(it).equals(path) && it.getLine() == line - 1) { // turning 1-indexed back to 0-indexed
                return it;
            }
        }
        return null;
    }

    public static String absolutePath(XLineBreakpoint<?> receiver) {
        return receiver.getFileUrl().replace("file://", "");
    }
}
