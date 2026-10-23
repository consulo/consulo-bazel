package org.jetbrains.bsp.protocol;

public final class BuildTargetTag {
    private BuildTargetTag() {
    }

    public static final String MANUAL = "manual";
    public static final String NO_IDE = "no-ide";
    public static final String MAVEN_COORDINATES = "maven_coordinates";
    public static final String MODULE_NAME_HINT = "ide-module-name";
}
