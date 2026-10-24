package org.jetbrains.bazel.sync.workspace.snapshot;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

// consulo: upstream @JvmInline value class
public final class WorkspaceConfigurationId {
    private final @Nullable String myShortChecksum;

    private WorkspaceConfigurationId(@Nullable String shortChecksum) {
        myShortChecksum = shortChecksum;
    }

    public @Nullable String getShortChecksum() {
        return myShortChecksum;
    }

    public static final WorkspaceConfigurationId EMPTY = new WorkspaceConfigurationId(null);

    // https://github.com/bazelbuild/bazel/blob/3dcf3fd7c59eaaf207ec342877d6e8978b903825/src/main/java/com/google/devtools/build/lib/analysis/config/BuildOptions.java#L263
    private static final int SHORT_ID_LENGTH = 7;

    // RC: force `WorkspaceConfigurationId` normalization to avoid
    //  unexpected failing equality checks, don't ask how I know...
    public static WorkspaceConfigurationId of(@Nullable String configurationChecksum) {
        if (configurationChecksum == null || configurationChecksum.isBlank()) {
            return EMPTY;
        }

        // normalize to short id
        else {
            return new WorkspaceConfigurationId(
                configurationChecksum.substring(0, Math.min(SHORT_ID_LENGTH, configurationChecksum.length()))
            );
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof WorkspaceConfigurationId that && Objects.equals(myShortChecksum, that.myShortChecksum);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(myShortChecksum);
    }

    @Override
    public String toString() {
        return "WorkspaceConfigurationId(shortChecksum=" + myShortChecksum + ")";
    }
}

// TODO: move `WorkspaceConfiguration` to backend after importer + connector merge
