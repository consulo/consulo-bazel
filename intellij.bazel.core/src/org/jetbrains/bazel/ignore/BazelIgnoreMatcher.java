package org.jetbrains.bazel.ignore;

import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileFactory;
import consulo.project.ProjectManager;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement;
import org.jetbrains.bazel.utils.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public interface BazelIgnoreMatcher {
    /**
     * Checks if given path matches the ignore criteria
     *
     * @param relativePath Path <b>relative</b> to repository root
     */
    boolean match(Path relativePath);

    BazelIgnoreMatcher EMPTY = relativePath -> false;

    final class BazelIgnoreMatcherFactory {
        private BazelIgnoreMatcherFactory() {
        }

        /**
         * https://bazel.build/run/bazelrc#bazel-behavior-files
         * <p>
         * You can specify directories within the workspace that you want Bazel to ignore,
         * such as related projects that use other build systems.
         * Place a file called .bazelignore at the root of the workspace and add the directories
         * you want Bazel to ignore, one per line. Entries are relative to the workspace root.
         */
        public static BazelIgnoreMatcher fromBazelIgnoreFile(String contents) {
            Set<Path> entries = new LinkedHashSet<>();
            for (String it : contents.lines().toList()) {
                if (it.isBlank()) {
                    continue;
                }
                if (it.startsWith("#")) {
                    continue;
                }
                entries.add(Path.of(it));
            }

            if (entries.isEmpty()) {
                return BazelIgnoreMatcher.EMPTY;
            }

            return relativePath -> PathUtil.isUnder(relativePath, entries);
        }

        /**
         * https://bazel.build/rules/lib/globals/repo
         * <p>
         * `ignore_directories`: this function takes a list of strings and a directory is ignored
         * if any of the given strings matches its repository-relative path according
         * to the semantics of the glob() function.
         */
        public static BazelIgnoreMatcher fromRepoBazelFile(String contents) {

            List<List<String>> patterns = ReadAction.nonBlocking(() -> {
                StarlarkFile fileElement = (StarlarkFile) PsiFileFactory.getInstance(ProjectManager.getInstance().getDefaultProject())
                    .createFileFromText(
                        "REPO.bazel",
                        StarlarkLanguage.StarlarkFileType.INSTANCE,
                        contents
                    );

                StarlarkCallExpression ignoreDirectoriesCall = null;
                for (StarlarkExpressionStatement it : fileElement.findChildrenByClass(StarlarkExpressionStatement.class)) {
                    StarlarkCallExpression call = it.callExpressionOrNull();
                    if (call != null && "ignore_directories".equals(call.getCalledFunctionName())) {
                        ignoreDirectoriesCall = call;
                        break;
                    }
                }
                if (ignoreDirectoriesCall == null) {
                    return List.<List<String>>of();
                }

                StarlarkArgumentList argumentList = ignoreDirectoriesCall.getArgumentList();
                StarlarkArgumentElement[] arguments = argumentList != null ? argumentList.getArguments() : null;
                PsiElement value = arguments != null && arguments.length == 1 ? arguments[0].getValue() : null;
                if (!(value instanceof StarlarkListLiteralExpression ignoreList)) {
                    return List.<List<String>>of();
                }

                List<List<String>> result = new ArrayList<>();
                for (StarlarkElement it : ignoreList.getElements()) {
                    if (it instanceof StarlarkStringLiteralExpression stringLiteral) {
                        result.add(Arrays.asList(stringLiteral.getStringContents().split("/", -1)));
                    }
                }
                return result;
            }).executeSynchronously();

            if (patterns.isEmpty()) {
                return BazelIgnoreMatcher.EMPTY;
            }

            return new BazelIgnoreMatcher() {
                private final ConcurrentHashMap<String, Pattern> myCache = StarlarkGlob.createCache();

                @Override
                public boolean match(Path relativePath) {
                    // consulo: Kotlin Path.invariantSeparatorsPathString
                    String pathString = relativePath.toString().replace('\\', '/');
                    for (List<String> pattern : patterns) {
                        if (StarlarkGlob.patternMatches(pattern, pathString, false, myCache)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    }
}
