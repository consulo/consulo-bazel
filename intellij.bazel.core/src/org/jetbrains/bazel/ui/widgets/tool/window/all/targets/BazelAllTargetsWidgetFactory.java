package org.jetbrains.bazel.ui.widgets.tool.window.all.targets;

import consulo.logging.Logger;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.JBLoadingPanel;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.target.BuildTargetUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanel;
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanelModel;
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelToolWindowPanel;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@ExtensionImpl
public class BazelAllTargetsWidgetFactory implements
    ToolWindowFactory,
    DumbAware {
    private static final Logger LOG = Logger.getInstance(BazelAllTargetsWidgetFactory.class);
    // consulo: Consulo's ToolWindowFactory has no manage(); it is started by createToolWindowContent(),
    // so the upstream contentRequested semaphore is not needed

    public BazelAllTargetsWidgetFactory() {
    }

    // consulo: XML attributes of the upstream <toolWindow> registration
    @Override
    public String getId() {
        return BazelPluginConstants.BAZEL_TOOLWINDOW_ID;
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return BazelPluginIcons.bazelToolWindow();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(BazelPluginConstants.BAZEL_DISPLAY_NAME);
    }

    @Override
    public boolean canCloseContents() {
        return false;
    }

    // consulo: isApplicableAsync -> validate (the tool window is unregistered when it returns false)
    @Override
    public boolean validate(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project);
    }

    // consulo: the first (EDT) part of the upstream manage(), run when the tool window is registered
    @Override
    @RequiredUIAccess
    public void init(Project project, ToolWindow toolWindow) {
        // double-check again
        boolean isBazelProject = org.jetbrains.bazel.config.project.isBazelProject(project);
        toolWindow.setAvailable(isBazelProject);
        // TODO consulo: ToolWindowManager.isStripeButtonShow() does not exist
        // if (isBazelProject && !toolWindowManager.isStripeButtonShow(toolWindow)) {
        //     // the only way to force adding a stripe button is to show the tool window, due to a platform API limitation
        //     toolWindow.show();
        // }
    }

    @Override
    @RequiredUIAccess
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        // consulo: the content manager is the tool window disposable
        JBLoadingPanel panel = new JBLoadingPanel(new BorderLayout(), toolWindow.getContentManager());
        panel.startLoading();
        toolWindow.getContentManager().addContent(ContentFactory.getInstance().createContent(panel, "", false));

        manage(project, toolWindow);
    }

    private void manage(Project project, ToolWindow toolWindow) {
        // consulo: MutableSharedFlow<Unit>(replay = 1, DROP_OLDEST) update requests merged with targetStorage.views,
        // throttled (300 ms) and collected one by one on a sequential executor
        UpdateRequests updateRequests = new UpdateRequests();
        BazelTargetsPanelModel model = new BazelTargetsPanelModel(updateRequests);
        ActionManager actionManager = ActionManager.getInstance();
        CompletableFuture<BazelTargetsPanel> targetPanel =
            CompletableFuture.supplyAsync(
                () -> {
                    SimpleToolWindowPanel windowPanel = new SimpleToolWindowPanel(true, true);
                    BazelToolWindowPanel.configureBazelToolWindowToolBar(model, actionManager, windowPanel, project);
                    BazelTargetsPanel panel = new BazelTargetsPanel(project, model);
                    windowPanel.setContent(panel);

                    Content content = Objects.requireNonNull(toolWindow.getContentManager().getContent(0));
                    JBLoadingPanel loadingPanel = (JBLoadingPanel) content.getComponent();
                    loadingPanel.stopLoading();
                    // consulo: upstream replaces the content component; Consulo does not redisplay a replaced component,
                    // so the panel is put into the loading panel
                    loadingPanel.add(windowPanel, BorderLayout.CENTER);
                    loadingPanel.revalidate();
                    loadingPanel.repaint();

                    return panel;
                },
                project.getUIAccess()
            );
        // consulo: upstream coroutine failures are reported by the coroutine scope; a CompletableFuture swallows them
        targetPanel.whenComplete((panel, error) -> {
            if (error != null) {
                LOG.error(error);
            }
        });

        TargetStorage targetStorage = project.getInstance(TargetStorage.class);
        updateRequests.setCollector(
            () -> updateVisibleTargets(targetStorage, project, model, targetPanel)
        );
        // update immediately, avoid any delays
        updateRequests.collectNow();
        Runnable removeViewsListener = targetStorage.getViews().addListener(view -> updateRequests.run());
        Disposer.register(toolWindow.getContentManager(), () -> {
            removeViewsListener.run();
            updateRequests.dispose();
        });
    }

    /**
     * consulo: replaces {@code merge(targetStorage.views, updateRequests).throttle(300).collectLatest { updateVisibleTargets(...) }}.
     * A request schedules one collection 300 ms later; requests arriving in the meantime are merged into it.
     */
    private static final class UpdateRequests implements Runnable {
        private static final long THROTTLE_MS = 300;

        private final ExecutorService myExecutor =
            Application.get().getInstance(ApplicationConcurrency.class).createSequentialApplicationPoolExecutor("BazelAllTargetsWidget");
        private final AtomicBoolean myScheduled = new AtomicBoolean();
        private volatile @Nullable Runnable myCollector;
        private volatile boolean myDisposed;

        void setCollector(Runnable collector) {
            myCollector = collector;
        }

        void collectNow() {
            myExecutor.execute(this::collect);
        }

        @Override
        public void run() {
            if (myDisposed || !myScheduled.compareAndSet(false, true)) {
                return;
            }
            Application.get().getInstance(ApplicationConcurrency.class).getScheduledExecutorService().schedule(
                () -> myExecutor.execute(() -> {
                    myScheduled.set(false);
                    collect();
                }),
                THROTTLE_MS,
                TimeUnit.MILLISECONDS
            );
        }

        private void collect() {
            Runnable collector = myCollector;
            if (!myDisposed && collector != null) {
                collector.run();
            }
        }

        void dispose() {
            myDisposed = true;
            myExecutor.shutdown();
        }
    }

    public static CompletableFuture<Void> showBspToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(BazelPluginConstants.BAZEL_TOOLWINDOW_ID);
        if (toolWindow == null) {
            return CompletableFuture.completedFuture(null);
        }
        // consulo: withContext(Dispatchers.EDT) { } -> the project UI access
        return CompletableFuture.runAsync(() -> toolWindow.show(), project.getUIAccess());
    }

    /**
     * Normally not needed, unless we link a Bazel project after {@link ToolWindowFactory#shouldBeAvailable} was called.
     */
    @SuppressWarnings("deprecation")
    public static CompletableFuture<Void> registerBazelToolWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow currentToolWindow = toolWindowManager.getToolWindow(getBazelToolWindowId());
        if (currentToolWindow == null) {
            return CompletableFuture.runAsync(
                () -> {
                    // consulo: no RegisterToolWindowTask with a contentFactory; the tool window is registered with the
                    // (deprecated) registerToolWindow and the content is created by the factory right away
                    BazelAllTargetsWidgetFactory contentFactory = new BazelAllTargetsWidgetFactory();
                    ToolWindow toolWindow = toolWindowManager.registerToolWindow(
                        getBazelToolWindowId(),
                        false,
                        ToolWindowAnchor.RIGHT,
                        project,
                        true
                    );
                    toolWindow.setIcon(BazelPluginIcons.bazelToolWindow());
                    contentFactory.createToolWindowContent(project, toolWindow);
                    toolWindow.show();
                },
                project.getUIAccess()
            );
        }
        return CompletableFuture.completedFuture(null);
    }

    private static String getBazelToolWindowId() {
        return BazelPluginConstants.BAZEL_DISPLAY_NAME;
    }

    private static CompletableFuture<Void> updateVisibleTargets(
        TargetStorage targetStorage,
        Project project,
        BazelTargetsPanelModel model,
        CompletableFuture<BazelTargetsPanel> targetPanel
    ) {
        // First, apply the filter
        RepoMapping repoMapping = targetStorage.repoMapping();
        Set<Label> distinctIds = new HashSet<>();
        List<Label> filteredTargets = TargetStorage.allTargetInfos(targetStorage)
            .stream()
            .filter(it -> distinctIds.add(it.id()))
            .filter(target -> BuildTargetUtils.isWorkspace(target, repoMapping) && model.getTargetFilter().predicate.test(target))
            .map(BuildTargetInfo::id)
            .toList();
        boolean hasAnyTargets = targetStorage.getTotalTargetCount() > 0;
        // Then, apply the search query
        @Nullable Pattern searchRegex;
        List<Label> searchResults;
        if (model.getSearchQuery().isEmpty()) {
            searchRegex = null;
            searchResults = filteredTargets;
        }
        else {
            int options = 0;
            if (!model.getMatchCase()) {
                // consulo: Kotlin RegexOption.IGNORE_CASE also enables UNICODE_CASE
                options |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            if (!model.getRegexMode()) {
                options |= Pattern.LITERAL;
            }

            Pattern regex = Pattern.compile(model.getSearchQuery(), options);
            searchRegex = regex;
            searchResults = filteredTargets.stream()
                .filter(target ->
                    regex.matcher(target.toString()).find() ||
                        regex.matcher(BazelRepoMappingUtils.toShortString(target, project)).find()
                )
                .toList();
        }

        // Finally, sort the results
        List<Label> visibleTargets = searchResults.stream()
            .sorted(Comparator.comparing(it -> BazelRepoMappingUtils.toShortString(it, project)))
            .toList();
        @Nullable Pattern finalSearchRegex = searchRegex;
        // consulo: targetPanel.await() + withContext(Dispatchers.EDT) { } -> the project UI access
        return targetPanel.thenCompose(panel -> CompletableFuture.runAsync(
            () -> panel.update(
                visibleTargets,
                finalSearchRegex,
                hasAnyTargets,
                model.getDisplayAsTree()
            ),
            project.getUIAccess()
        ));
    }
}
