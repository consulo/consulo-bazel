package org.jetbrains.bazel.languages.starlark.bazel;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.Main;
import org.jetbrains.bazel.label.Label.RepoType;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class BazelFileUtils {
    private static final int MAX_PATH_LENGTH = 25;
    private static final int MIN_SEGMENTS_NUMBER = 1;

    private BazelFileUtils() {
    }

    public static @Nullable String getContainingDirectoryPresentablePath(Project project, VirtualFile file) {
        ResolvedLabel calculated = BazelRepoMappingUtils.calculateLabel(project, file);
        Label apparent = calculated == null ? null : BazelRepoMappingUtils.toApparentLabelOrThis(calculated, project);
        if (!(apparent instanceof ResolvedLabel relativeDir)) {
            return null;
        }
        if (relativeDir.repo() instanceof Main && relativeDir.packagePath().pathSegments().isEmpty()) {
            return null;
        }
        String presentablePath = truncatePathHead(relativeDir);
        return file.getNameWithoutExtension() + " (" + presentablePath + ")";
    }

    private static String truncatePathHead(ResolvedLabel relativeDir) {
        List<String> segments = relativeDir.packagePath().pathSegments();
        int takenSegments = 0;
        int length = 0;
        // consulo: Kotlin takeLastWhile { }
        int firstTaken = segments.size();
        for (int i = segments.size() - 1; i >= 0; i--) {
            String segment = segments.get(i);
            length += segment.length();
            takenSegments++;
            if (!(length <= MAX_PATH_LENGTH || takenSegments <= MIN_SEGMENTS_NUMBER)) {
                break;
            }
            firstTaken = i;
        }
        List<String> tailSegments = segments.subList(firstTaken, segments.size());
        String tailString = String.join("/", tailSegments);
        RepoType repo = relativeDir.repo();
        String repoString = repo instanceof Main ? "" : repo.toString();
        return tailSegments.size() == segments.size() ? repoString + "//" + tailString : repoString + "//.../" + tailString;
    }
}
