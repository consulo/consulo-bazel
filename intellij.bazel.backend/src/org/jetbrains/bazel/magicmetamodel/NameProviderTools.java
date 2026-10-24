package org.jetbrains.bazel.magicmetamodel;

import consulo.project.Project;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bsp.protocol.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO: I really don't like `formatAsModuleName`, it suppose to construct `ModuleEntity`/`LibraryEntity`
//  name from label, but it misused e.g., in TargetUtils by using it to create key in module to target map,
//  this is bad because we end up with multiple places that "define" global module name, it isn't great
//  ideal solution would include obtaining those names only within backend modules and using those values as
//  source of truth

// consulo: upstream Kotlin top-level (extension) functions; the class keeps the upstream file name
public final class NameProviderTools {
    private NameProviderTools() {
    }

    public static String formatAsModuleName(Label receiver, Project project) {
        String targetName = sanitizeName(receiver.getTargetName());
        String prefix =
            shortenTargetPath(
                pathSegmentsWithRepoName(BazelRepoMappingUtils.toApparentLabelOrThis(receiver, project))
                    .stream()
                    .filter(pathSegment -> !pathSegment.isEmpty())
                    .toList(),
                targetName.length()
            )
                .stream()
                .map(pathElement -> sanitizeName(pathElement))
                .collect(Collectors.joining("."));
        return prefix.isBlank() ? targetName : prefix + "." + targetName;
    }

    public static String formatAsModuleName(Label receiver, RepoMapping repoMapping) {
        String targetName = sanitizeName(receiver.getTargetName());
        String prefix =
            shortenTargetPath(
                pathSegmentsWithRepoName(BazelRepoMappingUtils.toApparentLabelOrThis(receiver, repoMapping))
                    .stream()
                    .filter(pathSegment -> !pathSegment.isEmpty())
                    .toList(),
                targetName.length()
            )
                .stream()
                .map(pathElement -> sanitizeName(pathElement))
                .collect(Collectors.joining("."));
        return prefix.isBlank() ? targetName : prefix + "." + targetName;
    }

    // consulo: overload for the upstream default argument
    public static String formatAsModuleName(WorkspaceTargetKey receiver, RepoMapping repoMapping) {
        return formatAsModuleName(receiver, repoMapping, true);
    }

    public static String formatAsModuleName(WorkspaceTargetKey receiver, RepoMapping repoMapping, boolean withConfiguration) {
        if (withConfiguration) {
            @Nullable String shortChecksum = receiver.configuration().getShortChecksum();
            String checksum = shortChecksum != null ? shortChecksum : "default";
            return formatAsModuleName(receiver.label(), repoMapping) + "-" + checksum;
        }
        else {
            return formatAsModuleName(receiver.label(), repoMapping);
        }
    }

    // consulo: overload for the upstream default argument
    public static String formatAsLibraryName(WorkspaceTargetKey receiver, RepoMapping repoMapping) {
        return formatAsLibraryName(receiver, repoMapping, true);
    }

    public static String formatAsLibraryName(WorkspaceTargetKey receiver, RepoMapping repoMapping, boolean withFullKey) {
        String base = formatAsModuleName(receiver.label(), repoMapping);
        if (withFullKey) {
            List<String> aspectIds = receiver.aspectIds().getIds();
            @Nullable String aspectChecksum = !aspectIds.isEmpty() ? StringUtils.md5Hash(String.join(",", aspectIds), 5) : null;
            List<String> suffixParts = new ArrayList<>();
            @Nullable String shortChecksum = receiver.configuration().getShortChecksum();
            if (shortChecksum != null) {
                suffixParts.add(shortChecksum);
            }
            if (aspectChecksum != null) {
                suffixParts.add(aspectChecksum);
            }
            String suffix = String.join("-", suffixParts);
            if (suffix.isEmpty()) {
                return base;
            }
            else {
                return base + "-" + suffix;
            }
        }
        else {
            return base;
        }
    }

    // consulo: overload for the upstream default argument
    private static List<String> shortenTargetPath(List<String> receiver) {
        return shortenTargetPath(receiver, 0);
    }

    private static List<String> shortenTargetPath(List<String> receiver, int targetNameLength) {
        if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled()) {
            int maxLength = 200 - targetNameLength;
            int runningLength = 0;
            // consulo: Kotlin `asReversed().partition { ... }`
            List<String> subPath = new ArrayList<>();
            List<String> remaining = new ArrayList<>();
            for (int i = receiver.size() - 1; i >= 0; i--) {
                String it = receiver.get(i);
                runningLength += it.length();
                if (runningLength <= maxLength) {
                    subPath.add(it);
                }
                else {
                    remaining.add(it);
                }
            }
            if (remaining.isEmpty()) {
                return subPath.reversed();
            }
            else {
                List<String> result = new ArrayList<>();
                result.add(StringUtils.md5Hash(String.join("", remaining), 5));
                result.addAll(subPath.reversed());
                return result;
            }
        }
        else {
            return receiver;
        }
    }

    public static String sanitizeName(String receiver) {
        return filterColon(replaceDots(receiver));
    }

    private static String replaceDots(String receiver) {
        return receiver.replace('.', '-');
    }

    /**
     * for Windows compatibility, e.g. C:
     */
    private static String filterColon(String receiver) {
        return receiver.replace(":", "");
    }

    public static String shortenTargetPath(String receiver) {
        if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled()) {
            return String.join(".", shortenTargetPath(Arrays.asList(receiver.split("\\.", -1))));
        }
        else {
            return receiver;
        }
    }

    // consulo: Kotlin `.let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }`
    private static List<String> pathSegmentsWithRepoName(Label it) {
        List<String> result = new ArrayList<>();
        result.add(it instanceof ResolvedLabel resolvedLabel ? resolvedLabel.getRepoName() : "");
        result.addAll(it.packagePath().pathSegments());
        return result;
    }
}
