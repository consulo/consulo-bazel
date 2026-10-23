package org.jetbrains.bsp.protocol;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Request and response classes for local->remote resolution.
 */
public record BazelResolveLocalToRemoteParams(List<String> localPaths) {
    public record BazelResolveLocalToRemoteResult(
        /*
         * Maps each local path to the resolved remote path.
         */
        Map<String, String> resolvedPaths
    ) {
    }

    /**
     * Request and response classes for remote->local resolution.
     */
    public record BazelResolveRemoteToLocalParams(List<String> remotePaths, String goRoot) {
    }

    public record BazelResolveRemoteToLocalResult(
        /*
         * Maps each remote path to a local absolute path (or an empty string if not resolvable).
         */
        Map<String, Path> resolvedPaths
    ) {
    }
}
