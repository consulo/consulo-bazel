// consulo: package org.jetbrains.bazel.sync.workspace.mapper.normal is also a package of consulo.bazel.base (JPMS split package)
package org.jetbrains.bazel.sync.workspace.mapper.normal.java;

import org.jetbrains.bazel.label.Label;
import org.jetbrains.bsp.protocol.BuildTargetTag;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.MavenCoordinates;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class MavenCoordinatesResolver {
    private MavenCoordinatesResolver() {
    }

    private static final String MAVEN_COORDS_TAG_PREFIX = BuildTargetTag.MAVEN_COORDINATES + "=";

    public static @Nullable MavenCoordinates fromTargetTagsList(Collection<String> tags) {
        String mavenCoordsTag = null;
        for (String it : tags) {
            if (it.startsWith(MAVEN_COORDS_TAG_PREFIX)) {
                mavenCoordsTag = it.substring(MAVEN_COORDS_TAG_PREFIX.length());
                break;
            }
        }
        if (mavenCoordsTag == null) {
            return null;
        }

        // consulo: limit -1 keeps trailing empty strings like Kotlin split
        String[] parts = mavenCoordsTag.split(":", -1);
        if (parts.length < 1) {
            return null;
        }
        String groupId = parts[0];
        if (parts.length < 2) {
            return null;
        }
        String artifactId = parts[1];
        if (parts.length < 3) {
            return null;
        }
        String version = parts[2];
        return new MavenCoordinates(
            groupId,
            artifactId,
            version
        );
    }

    public static @Nullable MavenCoordinates resolveMavenCoordinates(Label libraryLabel, Path outputJar) {
        /* For example:
         * @@rules_jvm_external~override~maven~maven//:org_apache_commons_commons_lang3 -> org
         * @maven//:com_google_auto_service_auto_service_annotations -> com
         */
        String[] targetNameParts = libraryLabel
            .getTargetName()
            .split("_", 2);
        String topLevelDomain = targetNameParts.length > 0 ? targetNameParts[0] : "org";
        // Match the Maven group (organization), artifact, and version in the Bazel dependency
        // string such as .../execroot/monorepo/bazel-out/k8-fastbuild/bin/external/maven/com/google/guava/guava/31.1-jre/processed_guava-31.1-jre.jar
        // bazel-out/k8-fastbuild/bin/external/rules_jvm_external~~maven~name/v1/https/repo1.maven.org/maven2/com/google/auto/service/auto-service-annotations/1.1.1/header_auto-service-annotations-1.1.1.jar
        if (!"jar".equals(extension(outputJar))) {
            return null;
        }

        List<String> pathSegments = new ArrayList<>(outputJar.getNameCount());
        for (int i = 0; i < outputJar.getNameCount(); i++) {
            pathSegments.add(outputJar.getName(i).toString());
        }
        int organizationFromIndex = pathSegments.indexOf(topLevelDomain);
        if (organizationFromIndex == -1) {
            return null;
        }
        int organizationToIndex = pathSegments.size() - 3;
        if (organizationFromIndex >= organizationToIndex) {
            return null;
        }
        String organization = String.join(".", pathSegments.subList(organizationFromIndex, organizationToIndex));

        String artifact = pathSegments.get(pathSegments.size() - 3);
        String version = pathSegments.get(pathSegments.size() - 2);
        return new MavenCoordinates(organization, artifact, version);
    }

    // consulo: Kotlin Path.extension
    private static String extension(Path path) {
        Path fileName = path.getFileName();
        String name = fileName == null ? "" : fileName.toString();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex < 0 ? "" : name.substring(dotIndex + 1);
    }
}
