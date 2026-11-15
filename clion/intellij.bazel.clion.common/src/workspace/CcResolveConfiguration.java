package org.jetbrains.bazel.clion.workspace;

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * consulo: upstream declares this data class in buildEquivalenceClasses.kt; a public Java type needs a file of its own.
 * The sources are VFS url strings (upstream {@code VirtualFileUrl}).
 */
public record CcResolveConfiguration(
    Identifier id,
    String name,
    EquivalenceClass shared,
    List<WorkspaceTargetKey> targets,
    List<String> sources
) {

    /**
     * Represents a set of compiler settings that are shared across multiple targets.
     */
    public record EquivalenceClass(
        WorkspaceConfigurationId configuration,
        CcCompilerInfo compilerSettings,

        List<String> copts,
        List<String> conlyopts,
        List<String> cxxopts,

        OutputLocationCollection transitiveIncludes,
        OutputLocationCollection transitiveQuoteIncludes,
        OutputLocationCollection transitiveSystemIncludes,

        List<String> transitiveDefines
    ) {
    }

    /**
     * Wrapper class for the unique identifier for an {@code OCResolveConfiguration}
     * (consulo: {@code consulo.bazel.cpp.workspace.CcWorkspaceModel.Configuration}).
     * <p>
     * consulo: a class, not a record: a record component cannot be named {@code hashCode}
     */
    public static final class Identifier {
        private final int myHashCode;
        private final WorkspaceConfigurationId myConfigurationId;

        public Identifier(int hashCode, WorkspaceConfigurationId configurationId) {
            myHashCode = hashCode;
            myConfigurationId = configurationId;
        }

        public int getHashCode() {
            return myHashCode;
        }

        public WorkspaceConfigurationId getConfigurationId() {
            return myConfigurationId;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Identifier that)) {
                return false;
            }
            return myHashCode == that.myHashCode && myConfigurationId.equals(that.myConfigurationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myHashCode, myConfigurationId);
        }

        @Override
        public String toString() {
            return "Identifier(hashCode=" + myHashCode + ", configurationId=" + myConfigurationId + ")";
        }
    }
}
