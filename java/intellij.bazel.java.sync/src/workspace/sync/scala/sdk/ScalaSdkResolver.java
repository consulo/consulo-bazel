package org.jetbrains.bazel.scala.sdk;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import consulo.util.lang.Version;
import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScalaSdkResolver {
    private ScalaSdkResolver() {
    }

    public static @Nullable String resolveScalaVersion(TargetIdeInfo targetInfo) {
        if (!targetInfo.hasScalaTargetInfo()) {
            return null;
        }
        // consulo: Kotlin mapNotNull + maxByOrNull (the first maximum wins)
        String maxVersion = null;
        Version maxValue = null;
        for (ArtifactLocation it : targetInfo.getScalaTargetInfo().getCompilerClasspathList()) {
            String relativePath = it.getRelativePath();
            String version = extractVersion(relativePath.substring(relativePath.lastIndexOf('/') + 1));
            if (version == null) {
                continue;
            }
            Version parsed = Version.parseVersion(version);
            Version value = parsed != null ? parsed : new Version(0, 0, 0);
            if (maxValue == null || maxValue.compareTo(value) < 0) {
                maxVersion = version;
                maxValue = value;
            }
        }
        return maxVersion;
    }

    private static @Nullable String extractVersion(String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static final Pattern VERSION_PATTERN =
        Pattern.compile("(?:processed_)?scala3?-(?:library|compiler|reflect)(?:_3)?-([.\\d]+).*?\\.jar");
}
