package org.jetbrains.bazel.workspacemodel.entities;

// consulo: the IntelliJ workspace model is not used. Ownership of modules and libraries is tracked by
// consulo.bazel.workspace.BazelModelBuilder; these sources remain as markers for code ported from upstream.
public sealed interface BazelEntitySource
    permits BazelEntitySource.BazelProjectEntitySource, BazelEntitySource.BazelModuleEntitySource, BazelEntitySource.BazelDummyEntitySource {

    final class BazelProjectEntitySource implements BazelEntitySource {
        public static final BazelProjectEntitySource INSTANCE = new BazelProjectEntitySource();

        private BazelProjectEntitySource() {
        }

        @Override
        public String toString() {
            return "BazelProjectEntitySource";
        }
    }

    record BazelModuleEntitySource(String moduleName) implements BazelEntitySource {
    }

    final class BazelDummyEntitySource implements BazelEntitySource {
        public static final BazelDummyEntitySource INSTANCE = new BazelDummyEntitySource();

        private BazelDummyEntitySource() {
        }

        @Override
        public String toString() {
            return "BazelDummyEntitySource";
        }
    }
}
