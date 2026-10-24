package org.jetbrains.bazel.server.bsp.utils;

import org.jetbrains.bazel.commons.constants.Constants;

import java.nio.file.Path;

public class InternalAspectsResolver {
    private final Path myWorkspaceRoot;

    private final String myPrefix = "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/";

    public InternalAspectsResolver(Path workspaceRoot) {
        myWorkspaceRoot = workspaceRoot;
    }

    public Path getWorkspaceRoot() {
        return myWorkspaceRoot;
    }

    public String resolveLabel(String aspect) {
        return myPrefix + aspect;
    }
}
