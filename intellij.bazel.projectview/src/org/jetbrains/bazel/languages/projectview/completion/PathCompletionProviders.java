package org.jetbrains.bazel.languages.projectview.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.bazel.languages.projectview.ProjectViewCodeInsightSupport;
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection;
import org.jetbrains.bazel.sync.SyncCache;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.jetbrains.bazel.config.project.getRootDir;
import static org.jetbrains.bazel.config.project.isBazelProject;

// consulo: upstream top-level declarations of PathCompletionProviders.kt; the providers are used from
// org.jetbrains.bazel.languages.projectview, so they are public nested classes
public final class PathCompletionProviders {
    private PathCompletionProviders() {
    }

    // consulo: Kotlin default argument depthLimit = null
    private static List<String> getRelativePaths(Project receiver, Predicate<VirtualFile> filter) {
        return getRelativePaths(receiver, null, filter);
    }

    private static List<String> getRelativePaths(Project receiver, @Nullable Integer depthLimit, Predicate<VirtualFile> filter) {
        List<VirtualFile> result = new ArrayList<>();
        Set<VirtualFile> excludedRoots = receiver.getInstance(ProjectViewCodeInsightSupport.class).getExcludedRoots();
        VirtualFile rootDir = getRootDir(receiver);
        VirtualFileVisitor.Option[] options =
            depthLimit != null
                ? new VirtualFileVisitor.Option[]{VirtualFileVisitor.limit(depthLimit)}
                : new VirtualFileVisitor.Option[0];
        VirtualFileUtil.visitChildrenRecursively(
            rootDir,
            new VirtualFileVisitor<Object>(options) {
                @Override
                public Result visitFileEx(VirtualFile file) {
                    if (excludedRoots.contains(file)) {
                        return SKIP_CHILDREN;
                    }
                    if (filter.test(file)) {
                        result.add(file);
                    }
                    return CONTINUE;
                }
            }
        );
        List<String> paths = new ArrayList<>();
        for (VirtualFile it : result) {
            String path = VirtualFileUtil.getRelativePath(it, rootDir);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static final int DIRECTORIES_COMPLETION_DEPTH_LIMIT = 4;

    public static class DirectoriesCompletionProvider implements CompletionProvider {
        private final SyncCache.SyncCacheComputable<List<String>> myPathsComputable =
            project -> getRelativePaths(project, DIRECTORIES_COMPLETION_DEPTH_LIMIT, VirtualFile::isDirectory);

        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            Project project = parameters.getPosition().getProject();
            if (!isBazelProject(project)) {
                return;
            }
            ProjectViewPsiSection section = PsiTreeUtil.getParentOfType(parameters.getPosition(), ProjectViewPsiSection.class);
            if (section == null) {
                return;
            }
            Set<String> previousValues = section.getItems().stream().map(it -> it.getText().trim()).collect(Collectors.toSet());
            List<String> paths =
                SyncCache.getInstance(project).get(myPathsComputable).stream().filter(it -> !previousValues.contains(it)).toList();

            CompletionResultSet resultWithCustomPrefixMatcher = result.withPrefixMatcher(getPrefix(parameters));
            for (String it : paths) {
                resultWithCustomPrefixMatcher.addElement(
                    LookupElementBuilder
                        .create(it)
                        .withIcon(PlatformIconGroup.nodesFolder())
                );
            }
        }

        private String getPrefix(CompletionParameters receiver) {
            PsiElement position = receiver.getPosition();
            String text = position.getText();
            // consulo: Kotlin take(n).removePrefix("-")
            String taken = text.substring(0, Math.min(receiver.getOffset() - position.getTextRange().getStartOffset(), text.length()));
            return StringUtil.trimStart(taken, "-");
        }
    }

    public static class FiletypeCompletionProvider implements CompletionProvider {
        private final String myFileExtension;

        private final SyncCache.SyncCacheComputable<List<String>> myPathsComputable;

        public FiletypeCompletionProvider(String fileExtension) {
            myFileExtension = fileExtension;
            myPathsComputable = project -> getRelativePaths(project, it -> it.getName().endsWith(fileExtension));
        }

        public String getFileExtension() {
            return myFileExtension;
        }

        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            Project project = parameters.getPosition().getProject();
            if (!isBazelProject(project)) {
                return;
            }
            ProjectViewPsiSection section = PsiTreeUtil.getParentOfType(parameters.getPosition(), ProjectViewPsiSection.class);
            if (section == null) {
                return;
            }
            Set<String> previousValues = section.getItems().stream().map(it -> it.getText().trim()).collect(Collectors.toSet());
            List<String> paths = SyncCache.getInstance(project).get(myPathsComputable);
            String originalFileRelativePath = getOriginalFileRelativePath(parameters);
            paths.stream().filter(it -> !previousValues.contains(it) && !it.equals(originalFileRelativePath)).forEach(it ->
                result.addElement(
                    LookupElementBuilder
                        .create(it)
                        .withIcon(PlatformIconGroup.nodesFolder())
                )
            );
        }
    }

    public static class ImportCompletionProvider implements CompletionProvider {
        private final SyncCache.SyncCacheComputable<List<String>> myPathsComputable =
            project -> getRelativePaths(project, it -> it.getName().endsWith(".bazelproject"));

        @Override
        public void addCompletions(
            CompletionParameters parameters,
            ProcessingContext context,
            CompletionResultSet result
        ) {
            Project project = parameters.getPosition().getProject();
            if (!isBazelProject(project)) {
                return;
            }
            String originalFileRelativePath = getOriginalFileRelativePath(parameters);
            List<String> paths =
                SyncCache.getInstance(project).get(myPathsComputable).stream().filter(it -> !it.equals(originalFileRelativePath)).toList();

            result.addAllElements(paths.stream().map(LookupElementBuilder::create).toList());
        }
    }

    private static @Nullable String getOriginalFileRelativePath(CompletionParameters parameters) {
        return VirtualFileUtil.getRelativePath(
            Objects.requireNonNull(parameters.getOriginalFile().getVirtualFile()),
            getRootDir(parameters.getPosition().getProject())
        );
    }
}
