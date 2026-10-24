package org.jetbrains.bazel.workspace.packageMarker;

// consulo: upstream Kotlin top-level declarations; the class keeps the upstream file name
public final class concatenatePackages {
    private concatenatePackages() {
    }

    public static String concatenatePackages(String first, String second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + "." + second;
    }
}
