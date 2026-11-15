package org.jetbrains.bazel.debug.platform;

import consulo.execution.debug.breakpoint.XBreakpointProperties;

// File paths and line numbers are already automatically held in XLineBreakpointImpl,
// which represents IntelliJ breakpoints - that is why they are not defined explicitly here.
// This class seems useless, but needs to be kept nevertheless to satisfy XLineBreakpointType's type parameter
public class StarlarkBreakpointProperties extends XBreakpointProperties<StarlarkBreakpointProperties> {
    @Override
    public StarlarkBreakpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(StarlarkBreakpointProperties state) {
    }
}
