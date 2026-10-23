package org.jetbrains.bsp.protocol;

record GoDebuggerDataResult(
    String execRoot,
    String workspaceRoot,
    String outputBase
) {
}
