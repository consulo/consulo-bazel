package org.jetbrains.bazel.ui.projectTree;

// consulo: upstream declares the enum in BazelTreeStructureController.kt; it has its own file to keep the upstream FQN
public enum BazelTreeNodeType {
    ROOT,
    EXCLUDED,
    UNIMPORTED,
}
