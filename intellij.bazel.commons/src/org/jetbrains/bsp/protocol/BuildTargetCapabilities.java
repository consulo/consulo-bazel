package org.jetbrains.bsp.protocol;

record BuildTargetCapabilities(
    boolean canCompile,
    boolean canTest,
    boolean canRun
) {
    static boolean isExecutable(BuildTargetCapabilities receiver) {
        return receiver.canTest() || receiver.canRun();
    }
}
