package org.jetbrains.bazel.sync.workspace;

import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RepoMapping.LocalRepositoryMapping;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class DefaultOutputLocationResolver implements OutputLocationResolver {
    private final BazelInfo myBazelInfo;

    private DefaultOutputLocationResolver(BazelInfo bazelInfo) {
        myBazelInfo = bazelInfo;
    }

    public static OutputLocationResolver createLocalWorkspaceOnly(BazelInfo bazelInfo, RepoMapping repoMapping) {
        return new DefaultOutputLocationResolver(bazelInfo) {
            @Override
            public @Nullable Path resolve(OutputLocation location, @Nullable LocalRepositoryMapping localOverride) {
                if (!OutputLocation.isUserCode(location, repoMapping) || OutputLocation.isGenerated(location)) {
                    return null;
                }
                return super.resolve(location, localOverride);
            }
        };
    }

    // consulo: overload for the upstream default argument (hardlinks = BazelOutFileHardLinks.NONE)
    public static OutputLocationResolver createHardlinkResolving(BazelInfo bazelInfo) {
        return createHardlinkResolving(bazelInfo, BazelOutFileHardLinks.NONE);
    }

    public static OutputLocationResolver createHardlinkResolving(
        BazelInfo bazelInfo,
        BazelOutFileHardLinks hardlinks
    ) {
        return new DefaultOutputLocationResolver(bazelInfo) {
            @Override
            protected Path resolveExecrootPath(Path execrootPath) {
                return hardlinks.resolveCachedPath(execrootPath);
            }
        };
    }

    /**
     * Resolved locations can be located inside bazel execroot.
     */
    public static OutputLocationResolver createExecrootResolving(BazelInfo bazelInfo) {
        return new DefaultOutputLocationResolver(bazelInfo) {
        };
    }

    @Override
    public @Nullable Path resolve(
        OutputLocation location,
        @Nullable LocalRepositoryMapping localOverride
    ) {
        try {
            Path resolved = switch (location) {
                case OutputLocation.Host host -> Path.of(host.absolutePath());
                case OutputLocation.Workspace workspace -> myBazelInfo.workspaceRoot().resolve(workspace.relativePath());
                case OutputLocation.Output output -> myBazelInfo.execRoot().resolve(OutputLocation.toExecrootPath(output));
                case OutputLocation.External external -> resolveExternal(external, localOverride);
            };
            if (OutputLocation.isGenerated(location)) {
                return resolveExecrootPath(resolved);
            }
            else {
                return resolved;
            }
        }
        catch (InvalidPathException ignored) {
            return null;
        }
    }

    private Path resolveExternal(OutputLocation.External location, @Nullable LocalRepositoryMapping localOverride) {
        if (OutputLocation.isGenerated(location)) {
            return myBazelInfo.execRoot().resolve(OutputLocation.toExecrootPath(location)).normalize();
        }
        Path overriddenRoot = localOverride != null ? localOverride.localRepositories().get(location.repoName()) : null;
        if (overriddenRoot != null) {
            return myBazelInfo.workspaceRoot().resolve(overriddenRoot).resolve(location.relativePath()).normalize();
        }
        // a source file lives in the output base in both layouts
        return myBazelInfo.outputBase().resolve("external")
            .resolve(location.repoName())
            .resolve(location.relativePath());
    }

    protected Path resolveExecrootPath(Path execrootPath) {
        return execrootPath;
    }
}
