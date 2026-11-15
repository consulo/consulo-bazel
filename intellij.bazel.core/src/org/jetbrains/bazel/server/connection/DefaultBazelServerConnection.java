package org.jetbrains.bazel.server.connection;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver;
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher;
import org.jetbrains.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.BazelPathsResolver;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.install.EnvironmentCreator;
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral;
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionWorkspaceResolver;
import org.jetbrains.bazel.languages.projectview.ProjectView;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.server.BazelServerConnection;
import org.jetbrains.bazel.server.BazelServerFacade;
import org.jetbrains.bazel.server.bsp.BazelServerFacadeImpl;
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bazel.server.sync.BspProjectMapper;
import org.jetbrains.bazel.server.sync.ExecuteService;
import org.jetbrains.bazel.server.sync.ProjectResolver;
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver;
import org.jetbrains.bazel.sync.BazelEnvironmentService;
import org.jetbrains.bazel.sync.BazelOutFileHardLinks;
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks;
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService;
import org.jetbrains.bsp.protocol.OutputLocationParser;
import org.jetbrains.bsp.protocol.TaskId;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class DefaultBazelServerConnection implements BazelServerConnection {
    private final Project myProject;
    private final Path myWorkspaceRoot;
    private final EnvironmentCreator myEnvironmentCreator;

    DefaultBazelServerConnection(Project project) {
        myProject = project;
        myWorkspaceRoot = org.jetbrains.bazel.config.project.getRootDir(project).toNioPath();
        myEnvironmentCreator = new EnvironmentCreator(myWorkspaceRoot);
        myEnvironmentCreator.create();
    }

    record ServerWithVersionLiteral(@Nullable BazelServerFacadeImpl server, @Nullable BazelVersionLiteral versionLiteral) {
    }

    private final AtomicReference<ServerWithVersionLiteral> myServerAndVersionLiteral =
        new AtomicReference<>(new ServerWithVersionLiteral(null, null));

    @Override
    public <T extends @Nullable Object> CompletableFuture<T> runWithServer(
        @Nullable TaskId taskId,
        Function<BazelServerFacade, CompletableFuture<T>> task
    ) {
        return getServer(taskId).thenCompose(task::apply);
    }

    private CompletableFuture<BazelServerFacadeImpl> getServer(@Nullable TaskId taskId) {
        // ensure `.bazelbsp` directory exists and functions
        myEnvironmentCreator.create();

        ProjectView projectView = ProjectViewService.projectView(myProject);
        ServerWithVersionLiteral serverAndVersionLiteral = this.myServerAndVersionLiteral.get();
        BazelServerFacadeImpl server = serverAndVersionLiteral.server();
        BazelVersionLiteral oldVersionLiteral = serverAndVersionLiteral.versionLiteral();
        Path projectPath = org.jetbrains.bazel.config.project.getRootDir(myProject).toNioPath();
        BazelVersionLiteral resolvedVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(projectPath);
        boolean bazelVersionUpdated = !Objects.equals(oldVersionLiteral, resolvedVersion);

        if (server == null ||
            bazelVersionUpdated ||
            !server.getProjectView().equals(projectView)) {
            return createServer(projectView, taskId).thenApply(newServer -> {
                this.myServerAndVersionLiteral.set(new ServerWithVersionLiteral(newServer, resolvedVersion));
                return newServer;
            });
        }
        return CompletableFuture.completedFuture(server);
    }

    private CompletableFuture<BazelServerFacadeImpl> createServer(ProjectView projectView, @Nullable TaskId taskId) {
        BazelTaskEventsService taskEventsHandler = BazelTaskEventsService.getInstance(myProject);
        BazelInfoResolver bazelInfoResolver = new BazelInfoResolver(myWorkspaceRoot);
        BazelProcessLauncher.BazelProcessLauncherProvider bazelProcessLauncherProvider =
            BazelProcessLauncher.BazelProcessLauncherProvider.getInstance();
        return BazelEnvironmentService.getInstance(myProject).getEnvironment()
            .thenApply(environment -> bazelProcessLauncherProvider.createBazelProcessLauncher(myWorkspaceRoot, environment))
            .thenCompose(bazelProcessLauncher ->
                BazelRunner.create(myProject, taskEventsHandler, myWorkspaceRoot, bazelProcessLauncher, projectView)
            )
            .thenCompose(bazelRunner -> bazelInfoResolver.resolveBazelInfo(bazelRunner, projectView, taskId)
                .thenApply(bazelInfo -> createServer(projectView, taskEventsHandler, bazelRunner, bazelInfo)));
    }

    // consulo: the synchronous part of upstream createServer() after `bazel info` is resolved
    private BazelServerFacadeImpl createServer(
        ProjectView projectView,
        BazelTaskEventsService taskEventsHandler,
        BazelRunner bazelRunner,
        BazelInfo bazelInfo
    ) {
        BazelPathsResolver bazelPathsResolver = new BazelPathsResolver(bazelInfo);

        BazelOutFileHardLinks outFileHardLinks = BazelFeatureFlags.getHardLinkOutputFiles() &&
            !Application.get().getInstance(BazelApplicationContextService.class).getDisableHardLinksOutputFiles()
            ? new DefaultBazelOutputFileHardLinks(myProject, bazelInfo)
            : BazelOutFileHardLinks.NONE;

        ExecuteService executeService =
            new ExecuteService(
                /* project = */ myProject,
                /* workspaceRoot = */ myWorkspaceRoot,
                /* taskEventsHandler = */ taskEventsHandler,
                /* bazelRunner = */ bazelRunner,
                /* projectView = */ projectView,
                /* bazelPathsResolver = */ bazelPathsResolver
            );

        BazelBspAspectsManager bazelBspAspectsManager =
            new BazelBspAspectsManager(
                /* workspaceRoot = */ myWorkspaceRoot,
                /* executeService = */ executeService,
                /* bazelRelease = */ bazelInfo.release()
            );

        ProjectResolver projectResolver =
            new ProjectResolver(
                /* bazelBspAspectsManager = */ bazelBspAspectsManager,
                /* projectView = */ projectView,
                /* bazelInfo = */ bazelInfo,
                /* bazelRunner = */ bazelRunner,
                /* bazelPathsResolver = */ bazelPathsResolver,
                /* taskEventsHandler = */ taskEventsHandler,
                /* project = */ myProject
            );
        FirstPhaseProjectResolver firstPhaseProjectResolver =
            new FirstPhaseProjectResolver(
                /* workspaceRoot = */ myWorkspaceRoot,
                /* bazelRunner = */ bazelRunner,
                /* projectView = */ projectView,
                /* bazelInfo = */ bazelInfo
            );

        BspProjectMapper bspProjectMapper =
            new BspProjectMapper(
                /* workspaceRoot = */ myWorkspaceRoot,
                /* bazelRunner = */ bazelRunner,
                /* projectView = */ projectView,
                /* bazelPathsResolver = */ bazelPathsResolver
            );

        return new BazelServerFacadeImpl(
            /* bspMapper = */ bspProjectMapper,
            /* projectResolver = */ projectResolver,
            /* firstPhaseProjectResolver = */ firstPhaseProjectResolver,
            /* executeService = */ executeService,
            /* bazelRunner = */ bazelRunner,
            /* taskEventsHandler = */ taskEventsHandler,
            /* projectView = */ projectView,
            /* bazelInfo = */ bazelInfo,
            /* bazelPathsResolver = */ bazelPathsResolver,
            /* outFileHardLinks = */ outFileHardLinks,
            /* outputResolver = */ DefaultOutputLocationResolver.createHardlinkResolving(bazelInfo, outFileHardLinks),
            /* outputParser = */ new OutputLocationParser(bazelPathsResolver, outFileHardLinks)
        );
    }
}

@ServiceImpl
@Singleton
class BazelServerServiceImpl implements BazelServerConnection.BazelServerService {
    private final LazyValue<BazelServerConnection> myConnection;

    @Inject
    public BazelServerServiceImpl(Project project) {
        myConnection = LazyValue.atomicNotNull(() -> new DefaultBazelServerConnection(project));
    }

    @Override
    public BazelServerConnection getConnection() {
        return myConnection.get();
    }
}
