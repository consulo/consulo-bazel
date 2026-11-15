package org.jetbrains.bazel.extensionPoints.buildTargetClassifier;

import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface BuildTargetClassifierExtension {
    /**
     * Sets a separator for chaining target directories. Example:
     * <pre>
     * aaa
     *   | bbb
     *   | ccc
     *       | ddd
     *           | eee
     *               | BUILD_TARGET
     * </pre>
     * If the separator is set (for example to "+_"), the tree above will be rendered as:
     * <pre>
     * aaa
     *   | bbb
     *   | ccc+_ddd+_eee
     *       | BUILD_TARGET
     * </pre>
     * This affects only directories with just one child, this child being another directory
     */
    @Nullable String getSeparator();

    /**
     * @param buildTarget an intermediate representation of a BSP build target in `intellij-bsp`
     * @return list of directories corresponding to a desired path of the given build target in the tree.
     * For example, path `["aaa", "bbb"]` will render as:
     * <pre>
     * aaa
     *   | bbb
     *       | &lt;buildTarget&gt;
     * </pre>
     * If an empty list is returned, the build target will be rendered in the tree's top level
     */
    List<String> calculateBuildTargetPath(Label buildTarget);

    /**
     * @param buildTarget an intermediate representation of a BSP build target in `intellij-bsp`
     * @return the name under which the given build target will be rendered in the tree
     */
    String calculateBuildTargetName(Label buildTarget);

    class ListTargetClassifier implements BuildTargetClassifierExtension {
        private final Project myProject;

        public ListTargetClassifier(Project project) {
            myProject = project;
        }

        @Override
        public @Nullable String getSeparator() {
            return null;
        }

        @Override
        public List<String> calculateBuildTargetPath(Label buildTarget) {
            return List.of();
        }

        @Override
        public String calculateBuildTargetName(Label buildTarget) {
            return BazelRepoMappingUtils.toShortString(buildTarget, myProject);
        }
    }

    class TreeTargetClassifier implements BuildTargetClassifierExtension {
        private final Project myProject;

        public TreeTargetClassifier(Project project) {
            myProject = project;
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public List<String> calculateBuildTargetPath(Label buildTarget) {
            Label it = BazelRepoMappingUtils.toApparentLabelOrThis(buildTarget, myProject);
            List<String> segments = new ArrayList<>();
            segments.add(it instanceof Label.ResolvedLabel resolvedLabel ? resolvedLabel.getRepoName() : "");
            segments.addAll(it.packagePath().pathSegments());
            List<String> result = new ArrayList<>();
            for (String pathSegment : segments) {
                if (!pathSegment.isEmpty()) {
                    result.add(pathSegment);
                }
            }
            return result;
        }

        @Override
        public String calculateBuildTargetName(Label buildTarget) {
            return buildTarget.getTargetName();
        }
    }
}
