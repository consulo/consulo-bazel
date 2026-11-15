package org.jetbrains.bazel.run.synthetic;

import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.Label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class SyntheticRunTargetUtils {
    private SyntheticRunTargetUtils() {
    }

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

    // consulo: upstream signature is (vararg packageParts: String, targetName: String), a Java varargs parameter must be the last one
    public static Label getSyntheticTargetLabel(String[] packageParts, String targetName) {
        List<String> pathSegments = new ArrayList<>(List.of(Constants.DOT_BAZELBSP_DIR_NAME, Constants.SYNTHETIC_TARGETS_DIR_NAME));
        pathSegments.addAll(Arrays.asList(packageParts));
        return new Label.ResolvedLabel(
            /* repo = */ Label.Main.INSTANCE,
            /* packagePath = */ new Label.Package(pathSegments),
            /* target = */ new Label.SingleTarget(targetName)
        );
    }

    public static String escapeTargetLabel(String input) {
        return ESCAPE_PATTERN.matcher(input).replaceAll("_");
    }
}
