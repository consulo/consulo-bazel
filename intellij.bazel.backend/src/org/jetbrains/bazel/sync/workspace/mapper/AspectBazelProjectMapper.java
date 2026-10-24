package org.jetbrains.bazel.sync.workspace.mapper;

import com.google.devtools.intellij.aspect.Common.ArtifactLocation;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import com.intellij.aspect.lib.ReadTarget;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.interner.Interner;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.label.DependencyLabel;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.performance.BspTracer;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin;
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin.LanguageProjectMappers;
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection.OutputLocationCollectionBuilder;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceHelper;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget;
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler;
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler.BazelTaskLogger;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.BuildTarget.BuildTargetData;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationCollection;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AspectBazelProjectMapper {
    private final Project myProject;
    private final BazelServerFacade myServer;

    private final BazelPathsResolver myBazelPathsResolver;
    private final LanguageProjectMappers myLangMappers;

    public AspectBazelProjectMapper(
        Project project,
        BazelServerFacade server
    ) {
        myProject = project;
        myServer = server;
        myBazelPathsResolver = server.getBazelPathsResolver();
        myLangMappers = LanguagePlugin.createLanguageProjectMappers();
    }

    public CompletableFuture<List<BuildTarget>> mapTargets(
        Collection<Path> targetInfoPaths,
        boolean build,
        TaskId taskId
    ) {
        return BspTracer.measure("create raw targets", () -> {
            BazelTaskLogger taskLogger = BazelTaskEventsHandler.asLogger(BazelTaskEventsService.getInstance(myProject), taskId);
            // consulo: withContext(Dispatchers.Default) + mapNotNullConcurrent -> every path is read on the application pool,
            // the result keeps the order of targetInfoPaths
            List<CompletableFuture<@Nullable BuildTarget>> futures = new ArrayList<>();
            for (Path path : targetInfoPaths) {
                futures.add(
                    CompletableFuture.supplyAsync(
                        () -> ReadTarget.readTargetFromFile(
                            path,
                            msg -> taskLogger.error("Could not read target info " + path + ": " + msg)
                        ),
                        AppExecutorUtil.getAppExecutorService()
                    ).thenCompose(target -> {
                        if (target == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        List<String> pathSegments = Label.label(target).packagePath().pathSegments();
                        boolean isBazelBsp = !pathSegments.isEmpty() && pathSegments.get(0).equals(Constants.DOT_BAZELBSP_DIR_NAME);
                        if (isBazelBsp) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return createWorkspaceTarget(target, build, taskId);
                    })
                );
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenApply(ignored -> {
                List<BuildTarget> targets = new ArrayList<>();
                for (CompletableFuture<@Nullable BuildTarget> future : futures) {
                    BuildTarget target = future.join();
                    if (target != null) {
                        targets.add(target);
                    }
                }
                return targets;
            });
        });
    }

    private CompletableFuture<BuildTarget> createWorkspaceTarget(
        TargetIdeInfo target,
        boolean build,
        TaskId taskId
    ) {
        ResolvedLabel label = Label.assumeResolved(Label.label(target));
        TargetKind targetKind = inferTargetKind(target);

        CompletableFuture<List<BuildTargetData>> buildDataFuture = CompletableFuture.completedFuture(new ArrayList<>());
        for (LanguagePlugin plugin : myLangMappers.all()) {
            buildDataFuture = buildDataFuture.thenCompose(buildData -> plugin.mapBuildTargetData(myServer, target).thenApply(data -> {
                if (!plugin.getProvidedBuildTargetTypes().containsAll(data.stream().map(Object::getClass).toList())) {
                    throw new IllegalStateException(
                        "Language plugin " + plugin + " returned unregistered build target data: " +
                            data.stream().map(String::valueOf).collect(Collectors.joining(", "))
                    );
                }
                buildData.addAll(data);
                return buildData;
            }));
        }

        MissingFilesReporter missingFilesReporter = new MissingFilesReporter(myProject, taskId, label, build);

        // consulo: upstream local suspend fun resolveSourceSet
        BiFunction<List<ArtifactLocation>, Function<ArtifactLocation, MissingFileCategory>, CompletableFuture<OutputLocationCollection>>
            resolveSourceSet = (srcs, category) -> {
                List<ArtifactLocation> existing = new ArrayList<>();
                for (ArtifactLocation src : srcs) {
                    Path path = myBazelPathsResolver.resolve(src, RepoMapping.NoLocalRepositories);
                    if (!Files.exists(path)) {
                        missingFilesReporter.add(category.apply(src), src, path);
                        continue;
                    }
                    existing.add(src);
                }
                return OutputLocationCollectionBuilder.build(existing, myServer.getOutputParser());
            };

        return buildDataFuture.thenCompose(buildData -> resolveSourceSet.apply(
            target.getSrcsList(),
            src -> src.getIsSource() ? MissingFileCategory.SOURCES : MissingFileCategory.GENERATED_SOURCES
        ).thenCompose(sources -> resolveSourceSet.apply(
            target.getJvmTargetInfo().getResourcesList(),
            src -> MissingFileCategory.RESOURCES
        ).thenApply(resources -> {
            BuildTarget workspaceTarget = new WorkspaceTarget(
                WorkspaceHelper.toWorkspaceTargetKey(target.getKey()),
                target.getDepsList().stream().map(DependencyLabel::toDependencyLabel).toList(),
                targetKind,
                sources,
                resources,
                buildData,
                target.getGeneratorName(),
                target.getTestonly(),
                List.copyOf(target.getTagsList()),
                target.getWorkspaceName()
            );
            missingFilesReporter.report();
            return workspaceTarget;
        })));
    }

    private enum MissingFileCategory {
        SOURCES(
            "Source files",
            (file, build) -> {
                return LanguageClass.fromExtension(extension(file.path())) != null;
            }
        ),
        GENERATED_SOURCES(
            "Generated sources (files that were not materialized during sync, so they cannot be indexed by the IDE)",
            (file, build) -> {
                // report missing generated source only if build was requested
                return build && LanguageClass.fromExtension(extension(file.path())) != null;
            }
        ),
        RESOURCES(
            "Resources",
            (file, build) -> {
                return file.src().getIsSource() || build;
            }
        );

        private final String myTitle;
        private final BiPredicate<MissingFile, Boolean> myCondition;

        MissingFileCategory(String title, BiPredicate<MissingFile, Boolean> condition) {
            myTitle = title;
            myCondition = condition;
        }

        public String getTitle() {
            return myTitle;
        }

        public BiPredicate<MissingFile, Boolean> getCondition() {
            return myCondition;
        }

        // consulo: Kotlin Path.extension
        private static String extension(Path path) {
            Path fileName = path.getFileName();
            return fileName != null ? FileUtil.getExtension(fileName.toString()) : "";
        }
    }

    private static class MissingFilesReporter {
        private static final int DISPLAY_CAP = 16;

        private final Project myProject;
        private final TaskId myTaskId;
        private final Label myTarget;
        private final boolean myBuild;

        private final Map<MissingFileCategory, List<MissingFile>> myStored = new EnumMap<>(MissingFileCategory.class);
        private final int[] myTotals = new int[MissingFileCategory.values().length];

        MissingFilesReporter(
            Project project,
            TaskId taskId,
            Label target,
            boolean build
        ) {
            myProject = project;
            myTaskId = taskId;
            myTarget = target;
            myBuild = build;
            for (MissingFileCategory category : MissingFileCategory.values()) {
                myStored.put(category, new ArrayList<>());
            }
        }

        void add(MissingFileCategory category, ArtifactLocation src, Path path) {
            MissingFile file = new MissingFile(src, path);
            if (!category.getCondition().test(file, myBuild)) {
                return;
            }

            myTotals[category.ordinal()]++;
            List<MissingFile> list = Objects.requireNonNull(myStored.get(category));
            if (list.size() < DISPLAY_CAP) {
                list.add(file);
            }
        }

        void report() {
            int grandTotal = 0;
            for (int total : myTotals) {
                grandTotal += total;
            }
            if (grandTotal == 0) {
                return;
            }

            StringBuilder description = new StringBuilder();
            for (MissingFileCategory category : MissingFileCategory.values()) {
                int total = myTotals[category.ordinal()];
                if (total == 0) {
                    continue;
                }

                description.append(category.getTitle()).append('\n');
                List<MissingFile> files = Objects.requireNonNull(myStored.get(category));
                for (MissingFile file : files) {
                    description.append("  ")
                        .append(file.src().getRootPath()).append("/").append(file.src().getRelativePath())
                        .append(" (").append(file.path().toString()).append(")")
                        .append('\n');
                }
                if (total > files.size()) {
                    description.append("  ... and ").append(total - files.size()).append(" more\n");
                }
            }

            LOG.warn(
                "Target " + myTarget + " has missing " + grandTotal + " " + StringUtil.pluralize("file", grandTotal) + ": " + description
            );
        }
    }

    private record MissingFile(ArtifactLocation src, Path path) {
    }

    private static final Logger LOG = Logger.getInstance(AspectBazelProjectMapper.class);

    public static TargetKind inferTargetKind(TargetIdeInfo target) {
        TargetKind targetKind = TargetKindService.getInstance().guessFromRuleName(target.getKind());

        HashSet<LanguageClass> languages = new HashSet<>(targetKind.languageClasses());
        LanguagePlugin.getEpName().forEachExtensionSafe(plugin -> {
            languages.addAll(plugin.collectUsedLanguages(target));
        });

        RuleType ruleType;
        if (targetKind.ruleType() == RuleType.BINARY) {
            ruleType = RuleType.BINARY;
        }
        else if (targetKind.ruleType() == RuleType.TEST) {
            ruleType = RuleType.TEST;
        }
        else if (!target.hasExecutableInfo()) {
            ruleType = RuleType.LIBRARY;
        }
        else {
            ruleType = RuleType.BINARY;
        }

        return INTERNER.intern(new TargetKind(targetKind.kind(), languages, ruleType));
    }

    private static final Interner<TargetKind> INTERNER = Interner.createWeakInterner();
}
