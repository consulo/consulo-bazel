package org.jetbrains.bazel.commons;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.util.lang.Pair;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.ignore.BazelIgnoreService;
import org.jetbrains.bazel.label.Label;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Utility class for working with collections of includable and excludable targets.
 * This replaces the old TargetsSpec class.
 */
public record TargetCollection(List<Label> values, List<Label> excludedValues) {
    private static final Logger LOG = Logger.getInstance(TargetCollection.class);

    public TargetCollection(List<Label> values) {
        this(values, List.of());
    }

    public static TargetCollection fromExcludableList(List<ExcludableValue<Label>> targets) {
        List<Label> included = new ArrayList<>();
        List<Label> excluded = new ArrayList<>();

        targets.forEach(excludableTarget -> {
            if (excludableTarget.isIncluded()) {
                included.add(excludableTarget.value());
            }
            else {
                excluded.add(excludableTarget.value());
            }
        });

        return new TargetCollection(included, excluded);
    }

    /**
     * Removes each excluded recursive pattern of the main workspace that has no `BUILD` file beneath its directory.
     * <p>
     * Bazel rejects such a negative pattern with "no targets found beneath", and the pattern excludes nothing.
     * The walk skips the directories that {@code ignoreService} ignores and stops at the first `BUILD` file.
     * A nested module counts like any other directory, because Bazel includes its packages in the recursive pattern.
     * The walk does not follow symlinks, but Bazel does.
     * Thus a symlink to a directory, or a symlink with a `BUILD` file name, counts as a possible `BUILD` file.
     * A directory that the walk cannot read also keeps its pattern. See BAZEL-3586.
     */
    public static CompletableFuture<TargetCollection> dropExcludedPatternsWithoutPackages(
        TargetCollection receiver,
        Path workspaceRoot,
        BazelIgnoreService ignoreService
    ) {
        // consulo: upstream withContext(Dispatchers.IO)
        return CompletableFuture.supplyAsync(() -> {
            Set<Label> patternsWithoutPackages = receiver.excludedValues()
                .stream()
                .filter(it -> it.isRecursive() && it.isMainWorkspace())
                .filter(it -> !hasBuildFileBeneath(workspaceRoot.resolve(Label.toPath(it.packagePath())), ignoreService))
                .collect(Collectors.toCollection(HashSet::new));
            if (patternsWithoutPackages.isEmpty()) {
                return receiver;
            }

            LOG.info("Dropping the excluded patterns without packages: " + patternsWithoutPackages);
            return new TargetCollection(
                receiver.values(),
                receiver.excludedValues().stream().filter(it -> !patternsWithoutPackages.contains(it)).toList()
            );
        }, Application.get().getInstance(ApplicationConcurrency.class).getExecutorService());
    }

    // consulo: upstream passes the CoroutineContext for ensureActive(), ProgressManager.checkCanceled() is used instead
    private static boolean hasBuildFileBeneath(Path root, BazelIgnoreService ignoreService) {
        if (!Files.isDirectory(root)) {
            return false;
        }
        if (Files.isSymbolicLink(root)) {
            return true;
        }

        Deque<Path> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            ProgressManager.checkCanceled();
            Path directory = queue.removeFirst();
            if (ignoreService.isIgnored(directory)) {
                continue;
            }

            List<Pair<Path, BasicFileAttributes>> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path it : stream) {
                    entries.add(Pair.create(it, Files.readAttributes(it, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)));
                }
            }
            catch (IOException e) {
                LOG.info("Keeping the excluded pattern for " + root + ", because " + directory + " cannot be read", e);
                return true;
            }

            for (Pair<Path, BasicFileAttributes> entry : entries) {
                Path path = entry.getFirst();
                BasicFileAttributes attributes = entry.getSecond();
                boolean isBuildFileName =
                    Arrays.asList(Constants.BUILD_FILE_NAMES).contains(Objects.requireNonNull(path.getFileName()).toString());
                // With NOFOLLOW_LINKS, a symlink to a directory is not a directory, so check the symlink first.
                if (attributes.isSymbolicLink()) {
                    if (isBuildFileName || Files.isDirectory(path)) {
                        return true;
                    }
                }
                else if (attributes.isDirectory()) {
                    queue.add(path);
                }
                else if (isBuildFileName) {
                    return true;
                }
            }
        }
        return false;
    }
}
