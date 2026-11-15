package org.jetbrains.bazel.languages.bazelquery.completion;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jetbrains.bazel.config.project.getRootDir;

public class TargetCompletionsGenerator {
    private final Project myProject;
    private final String mySeparator = "/";
    private final String myStartTargetSign = "//";

    // TODO: check if it is possible to also consider external targets - BAZEL-2028
    private final Set<String> myAllTargets;

    public TargetCompletionsGenerator(Project project) {
        myProject = project;
        myAllTargets =
            TargetStorage.getTargetStorage(project)
                .allTargets()
                .map(it -> BazelRepoMappingUtils.toShortString(it, project))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // consulo: upstream default argument directory = null
    public List<String> getTargetsList(String prefix) {
        return getTargetsList(prefix, null);
    }

    public List<String> getTargetsList(String prefix, @Nullable Path directory) {
        // Note: project root dependent format - starts with "//",
        // directory dependent format - starts with a letter or ":"
        List<String> suggestions = new ArrayList<>();
        Path projectPath = Path.of(getRootDir(myProject).getPath());
        if (directory != null && !directory.startsWith(projectPath)) {
            return suggestions;
        }

        String currentDir = directory != null ? projectPath.relativize(directory).toString() : "";
        String prefixPath = myStartTargetSign + currentDir;

        List<String> allTargetsDirDepFormat;
        if (!currentDir.isEmpty()) {
            allTargetsDirDepFormat =
                myAllTargets
                    .stream()
                    .filter(it -> it.startsWith(prefixPath + mySeparator))
                    .map(it -> StringUtil.trimStart(it, prefixPath + mySeparator))
                    .toList();
        }
        else {
            allTargetsDirDepFormat = myAllTargets.stream().map(it -> StringUtil.trimStart(it, myStartTargetSign)).toList();
        }
        myAllTargets.addAll(allTargetsDirDepFormat);

        suggestions.addAll(myAllTargets.stream().filter(it -> it.startsWith(prefix)).toList());
        suggestions.addAll(generateTargetPatterns(prefix, suggestions));

        if (prefix.startsWith(mySeparator) || prefix.isEmpty()) {
            // All rule targets in packages in the main repository. Does not include targets from external repositories.
            suggestions.add(myStartTargetSign + "...");
            // All rule targets in the top-level package, if there is a `BUILD` file at the root of the workspace.
            suggestions.add(myStartTargetSign + ":all");
        }
        if (prefix.startsWith(":") || prefix.isEmpty()) {
            // All rule targets in the working directory.
            suggestions.add(":all");
            if (!prefix.isEmpty()) {
                // Targets in the working directory in directory dependent format (start with ":$prefix")
                suggestions.addAll(
                    myAllTargets
                        .stream()
                        .filter(it -> it.startsWith(prefixPath + prefix))
                        .map(it -> StringUtil.trimStart(it, prefixPath))
                        .toList()
                );
            }
            else {
                // Targets in the working directory in directory dependent format (start with ":")
                suggestions.addAll(
                    myAllTargets
                        .stream()
                        .filter(it -> it.startsWith(prefixPath + ":"))
                        .map(it -> StringUtil.trimStart(it, prefixPath))
                        .toList()
                );
            }
        }
        if (prefix.startsWith(".") || prefix.isEmpty()) {
            // All rule targets in all packages beneath the working directory.
            suggestions.add("...:all");
        }
        // Note: The following are not supported in completions:
        // bar/wiz - Equivalent to (assuming foo is current working directory):
        //    foo/bar/wiz:wiz if foo/bar/wiz is a package
        //    foo/bar:wiz if foo/bar is a package
        //    foo:bar/wiz otherwise

        return suggestions.stream().distinct().toList();
    }

    private List<String> generateTargetPatterns(String prefix, List<String> suggestions) {
        List<String> patterns = new ArrayList<>();
        List<String> availableSuggestions = new ArrayList<>(suggestions);
        // consulo: Kotlin String.split(delimiter) keeps trailing empty strings, hence the -1 limit
        int prefixSegments =
            prefix.isEmpty()
                ? 0
                : StringUtil.trimStart(StringUtil.trimStart(prefix, mySeparator), mySeparator)
                    .split(":", -1)[0]
                    .split(mySeparator, -1)
                    .length;

        availableSuggestions
            .forEach(s -> {
                patterns.add(s); // add single target suggestion
                boolean isProjectRootDependentFormat = s.startsWith(myStartTargetSign);
                List<String> suggestionSegments =
                    Arrays.asList(StringUtil.trimStart(s, myStartTargetSign).split(":", -1)[0].split(mySeparator, -1));
                for (int i = suggestionSegments.size() - 1; i >= 0; i--) {
                    if (!(i + 1 >= prefixSegments)) {
                        continue;
                    }
                    // add wildcard patterns for the subpath of target suggestion
                    if (isProjectRootDependentFormat) {
                        String path = myStartTargetSign + String.join(mySeparator, suggestionSegments.subList(0, i + 1));
                        patterns.add(path + ":all");
                        patterns.add(path + mySeparator + "...:all-targets");
                        patterns.add(path + mySeparator + "...:all");
                    }
                    else {
                        String path = String.join(mySeparator, suggestionSegments.subList(0, i + 1));
                        patterns.add(path + ":all");
                        patterns.add(path + mySeparator + "...:all");
                    }
                }
            });

        return patterns;
    }
}
