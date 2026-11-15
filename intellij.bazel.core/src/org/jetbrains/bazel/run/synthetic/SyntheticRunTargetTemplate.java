package org.jetbrains.bazel.run.synthetic;

public record SyntheticRunTargetTemplate(
    String buildFileContent,
    String buildFilePath
) {
}
