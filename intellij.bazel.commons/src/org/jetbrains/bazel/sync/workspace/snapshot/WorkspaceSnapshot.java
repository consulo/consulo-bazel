package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.util.interner.Interner;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

// consulo: holder class for the top-level declarations of upstream WorkspaceSnapshot.kt
public final class WorkspaceSnapshot {
    private WorkspaceSnapshot() {
    }

    /**
     * Unique target identifier
     *
     * @param label         Bazel target label
     * @param configuration Unique target configuration identifier can be empty
     */
    public record WorkspaceTargetKey(
        Label label,
        WorkspaceConfigurationId configuration,
        WorkspaceAspectIds aspectIds
    ) {
        public WorkspaceTargetKey(Label label) {
            this(label, WorkspaceConfigurationId.EMPTY);
        }

        public WorkspaceTargetKey(Label label, WorkspaceConfigurationId configuration) {
            this(label, configuration, WorkspaceAspectIds.EMPTY);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TargetKey(");
            builder.append(label);
            if (!configuration.equals(WorkspaceConfigurationId.EMPTY)) {
                builder.append(", conf=").append(configuration);
            }
            if (!aspectIds.equals(WorkspaceAspectIds.EMPTY)) {
                builder.append(", aspects=").append(String.join(";", aspectIds.getIds()));
            }
            builder.append(')');
            return builder.toString();
        }
    }

    // consulo: upstream @JvmInline value class
    public static final class WorkspaceAspectIds {
        private final List<String> myIds;

        private WorkspaceAspectIds(List<String> ids) {
            myIds = ids;
        }

        public List<String> getIds() {
            return myIds;
        }

        public static final WorkspaceAspectIds EMPTY = new WorkspaceAspectIds(List.of());

        private static final Interner<WorkspaceAspectIds> INTERNER = Interner.createWeakInterner();

        public static WorkspaceAspectIds of(List<String> aspectIds) {
            if (aspectIds.isEmpty() || aspectIds.stream().allMatch(String::isBlank)) {
                return EMPTY;
            }
            else {
                return INTERNER.intern(new WorkspaceAspectIds(aspectIds.stream().filter(it -> !it.isBlank()).toList()));
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o instanceof WorkspaceAspectIds that && myIds.equals(that.myIds);
        }

        @Override
        public int hashCode() {
            return myIds.hashCode();
        }

        @Override
        public String toString() {
            return "WorkspaceAspectIds(ids=" + myIds + ")";
        }
    }

    // consulo: upstream @JvmInline value class
    public static final class WorkspaceConfigurationId {
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

    /**
     * Definition of bazel configuration
     *
     * @param id        Unique configuration identifier
     * @param summary   Configuration summary taken from BEP
     * @param fragments Bazel configuration fragments
     */
    public record WorkspaceConfiguration(
        WorkspaceConfigurationId id,
        WorkspaceConfigurationSummary summary,
        List<WorkspaceConfigurationFragment> fragments
    ) {
    }

    /**
     * Bazel configuration summary emitted from BEP message
     *
     * @param hash         Full configuration hash
     * @param mnemonic     Platform mnemonic
     * @param platformName Platform name
     * @param cpu          Platform cpu
     * @param isTool       Whether this configuration is used for building tools
     */
    public record WorkspaceConfigurationSummary(
        String hash,
        String mnemonic,
        String platformName,
        String cpu,
        boolean isTool
    ) {
    }

    /**
     * Marker interface for bazel configuration fragments
     * <p>
     * <b>NOTE:</b> Each rule-specific fragment definition shall live in dedicated module
     */
    public interface WorkspaceConfigurationFragment {
    }
}
