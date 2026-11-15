package org.jetbrains.bazel.ui.console.filter;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ui.console.ConsoleFilterProvider;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A better version of {@link consulo.execution.ui.console.RegexpFilter} which supports relative paths as well
 * <p>
 * It tries to match:
 * <ul>
 *  <li>absolute path if exists, both Unix-style ({@code /home/user/a.kt}) and Windows-style with a drive letter
 *  ({@code C:&#92;Users&#92;user&#92;a.kt})</li>
 *  <li>relative path if exists and is not in the root of the project, why?
 *    Because then we can get a lot of false positives with just normal names in the console
 *    e.g. word "Build" could be matched to a BUILD file in the project root.
 *    So to avoid such cases, we do not mach anything in the root of the project -
 *    it's highly unlikely to have a non-path word in the console which has slash in it
 *    and maps to an existing file</li>
 * </ul>
 * <p>
 * Anything with two consecutive separators is not considered a path, so a bazel target
 * ({@code //plugins/bazel:bazel}, {@code @rules_java//java:java}), a UNC path or a {@code file://} URL is never matched.
 */
public class AbsoluteAndRelativePathsConsoleFilter implements Filter {
    private static final String PATH_GROUP_ID = "path";
    private static final String LINE_GROUP_ID = "line";
    private static final String COLUMN_GROUP_ID = "column";

    // consulo: Kotlin raw string with $PATH_GROUP_ID interpolation -> text block (backslashes doubled) with replace()
    private static final Pattern PATH_REGEX = Pattern.compile(
        """
                (?<![\\w.+/\\\\-])              # not preceded by a path character, so we never match a suffix of a longer path
                (?<$PATH_GROUP_ID>
                  (?:[A-Za-z]:)?             # optional Windows drive letter, e.g. C:
                  [/\\\\]?                     # optional leading separator of an absolute path
                  [\\w.+-]+                   # the first segment
                  (?:[/\\\\][\\w.+-]+)*         # the following segments, each after exactly one separator
                )
            """.replace("$PATH_GROUP_ID", PATH_GROUP_ID),
        Pattern.COMMENTS | Pattern.MULTILINE
    );

    private static final Pattern WINDOWS_ABSOLUTE_PATH_REGEX = Pattern.compile("[A-Za-z]:[/\\\\]");

    private static final Pattern POSITION_REGEX = Pattern.compile(":(?<" + LINE_GROUP_ID + ">\\d+):(?<" + COLUMN_GROUP_ID + ">\\d+)");
    private static final Pattern INTELLIJ_POSITION_REGEX =
        Pattern.compile(" \\((?<" + LINE_GROUP_ID + ">\\d+):(?<" + COLUMN_GROUP_ID + ">\\d+)\\)");

    private final Project myProject;

    public AbsoluteAndRelativePathsConsoleFilter(Project project) {
        myProject = project;
    }

    @Override
    public Result applyFilter(String line, int entireLength) {
        List<ResultItem> resultItems = new ArrayList<>();
        Matcher matcher = PATH_REGEX.matcher(line);
        while (matcher.find()) {
            Result result = toFilterResultOrNull(matcher, line, entireLength);
            if (result != null) {
                resultItems.add(result);
            }
        }
        return new Result(resultItems);
    }

    private @Nullable Result toFilterResultOrNull(Matcher match, String line, int entireLength) {
        String pathGroup = match.group(PATH_GROUP_ID);
        if (pathGroup == null) {
            return null;
        }
        int pathGroupStart = match.start(PATH_GROUP_ID);
        VirtualFile virtualFile = toVirtualFileInTheProject(pathGroup);
        if (virtualFile == null) {
            return null;
        }
        Matcher positionRegexResult = matchAt(POSITION_REGEX, line, match.end());
        if (positionRegexResult == null) {
            positionRegexResult = matchAt(INTELLIJ_POSITION_REGEX, line, match.end());
        }
        OpenFileHyperlinkInfo info = calculateInfo(virtualFile, positionRegexResult);
        int highlightStartOffset = calculateStartOffset(entireLength, line.length(), pathGroupStart);
        int highlightEndOffset = calculateEndOffset(highlightStartOffset, pathGroup, positionRegexResult);
        return new Result(highlightStartOffset, highlightEndOffset, info);
    }

    private @Nullable VirtualFile toVirtualFileInTheProject(String receiver) {
        if (receiver.indexOf('/') < 0 && receiver.indexOf('\\') < 0) {
            return null;
        }
        String path = toAbsolutePath(receiver);
        // consulo: no LocalFileSystem.findFileByPathWithoutCaching()
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile.exists() ? virtualFile : null;
    }

    private String toAbsolutePath(String receiver) {
        return isAbsolutePath(receiver) ? receiver : org.jetbrains.bazel.config.project.getRootDir(myProject).getPath() + "/" + receiver;
    }

    private static boolean isAbsolutePath(String receiver) {
        return receiver.startsWith("/") || receiver.startsWith("\\") || matchAt(WINDOWS_ABSOLUTE_PATH_REGEX, receiver, 0) != null;
    }

    private OpenFileHyperlinkInfo calculateInfo(VirtualFile virtualFile, @Nullable Matcher positionRegexResult) {
        int lineNumber = parseNumberOrZero(positionRegexResult, LINE_GROUP_ID);
        int columnNumber = parseNumberOrZero(positionRegexResult, COLUMN_GROUP_ID);
        return new OpenFileHyperlinkInfo(myProject, virtualFile, lineNumber, columnNumber);
    }

    // consulo: upstream positionRegexResult?.groups[name]?.value?.toIntOrNull()?.dec() ?: 0
    private static int parseNumberOrZero(@Nullable Matcher positionRegexResult, String groupName) {
        if (positionRegexResult == null) {
            return 0;
        }
        String value = positionRegexResult.group(groupName);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value) - 1;
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private int calculateStartOffset(
        int entireLength,
        int lineLength,
        int pathGroupStart
    ) {
        return entireLength - lineLength + pathGroupStart;
    }

    private int calculateEndOffset(
        int highlightStartOffset,
        String pathGroup,
        @Nullable Matcher locationRegexResult
    ) {
        return highlightStartOffset + pathGroup.length() + (locationRegexResult != null ? locationRegexResult.group().length() : 0);
    }

    // consulo: Kotlin Regex.matchAt(input, index)
    private static @Nullable Matcher matchAt(Pattern pattern, String input, int index) {
        Matcher matcher = pattern.matcher(input).useAnchoringBounds(false).useTransparentBounds(true).region(index, input.length());
        return matcher.lookingAt() ? matcher : null;
    }
}

@ExtensionImpl
class AbsoluteAndRelativePathsConsoleFilterProvider implements ConsoleFilterProvider {
    public AbsoluteAndRelativePathsConsoleFilterProvider() {
    }

    @Override
    public Filter[] getDefaultFilters(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project)
            ? new Filter[]{new AbsoluteAndRelativePathsConsoleFilter(project)}
            : Filter.EMPTY_ARRAY;
    }
}
