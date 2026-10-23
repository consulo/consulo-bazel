package org.jetbrains.bazel.label;

import java.util.regex.Pattern;

public final class LabelValidator {
    private LabelValidator() {
    }

    /**
     * Follows the Bazel doc - https://bazel.build/concepts/labels#target-names
     */
    public static boolean isTargetNameValid(String targetName) {
        return !targetName.isEmpty() && VALID_TARGET_NAME_PATTERN.matcher(targetName).matches();
    }

    private static final Pattern VALID_TARGET_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9!%\\-@^_\"#$&'()*+,;<=>?\\[\\]{}|~/.]+");
}
