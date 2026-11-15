package org.jetbrains.bazel.ui.widgets;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.searchEverywhere.FoundItemDescriptor;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereContributorFactory;
import consulo.searchEverywhere.SearchEverywherePreviewProvider;
import consulo.searchEverywhere.WeightedSearchEverywhereContributor;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import org.jetbrains.bazel.assets.BazelPluginIcons;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.target.TargetStorage;
import org.jspecify.annotations.Nullable;

import javax.swing.ListCellRenderer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class LabelSearchEverywhereContributor implements
    WeightedSearchEverywhereContributor<LabelSearchEverywhereContributor.LabelWithPreview>,
    SearchEverywherePreviewProvider {
    private final Project myProject;

    private final ListCellRenderer<LabelWithPreview> myListCellRenderer =
        SimpleListCellRenderer.create((label, value, index) -> {
            label.setText(value.getDisplayName());
            label.setIcon(BazelPluginIcons.bazel());
        });

    public LabelSearchEverywhereContributor(Project project) {
        myProject = project;
    }

    @Override
    public String getSearchProviderId() {
        return getClass().getSimpleName();
    }

    @Override
    public String getGroupName() {
        return BazelPluginConstants.BAZEL_DISPLAY_NAME;
    }

    @Override
    public int getSortWeight() {
        return 0;
    }

    @Override
    public boolean showInFindResults() {
        return true;
    }

    @Override
    public void fetchWeightedElements(
        String pattern,
        ProgressIndicator progressIndicator,
        Predicate<? super FoundItemDescriptor<LabelWithPreview>> consumer
    ) {
        MinusculeMatcher matcher =
            NameUtil
                .buildMatcher("*" + pattern)
                .withSeparators("@/:")
                .typoTolerant()
                // consulo: MatchingMode.IGNORE_CASE -> MatchingCaseSensitivity.NONE
                .withCaseSensitivity(NameUtil.MatchingCaseSensitivity.NONE)
                .build();

        List<FoundItemDescriptor<LabelWithPreview>> foundItems =
            TargetStorage.getTargetStorage(myProject)
                .allTargets()
                .<@Nullable FoundItemDescriptor<LabelWithPreview>>map(label -> {
                    String fullString = label.toString();
                    if (!matcher.matches(fullString)) {
                        return null;
                    }
                    PsiElement targetElement = ReadAction.<@Nullable PsiElement, RuntimeException>compute(
                        () -> LabelResolveUtils.resolveLabel(myProject, label, null, false)
                    );
                    if (targetElement == null) {
                        return null;
                    }
                    String displayName = BazelRepoMappingUtils.toShortString(label, myProject);
                    return new FoundItemDescriptor<>(new LabelWithPreview(displayName, targetElement), matcher.matchingDegree(fullString));
                })
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .toList();

        // Copied from AbstractGotoSEContributor
        // TODO consulo: ProgressIndicatorUtils.yieldToPendingWriteActions() and ModalityState.defaultModalityState() are not public API
        // if (ModalityState.defaultModalityState() == ModalityState.nonModal()) {
        //     ProgressIndicatorUtils.yieldToPendingWriteActions();
        // }
        // consulo: ProgressIndicatorUtils.runInReadActionWithWriteActionPriority -> ProgressManager
        ProgressManager.getInstance().runInReadActionWithWriteActionPriority(
            () -> ContainerUtil.process(foundItems, consumer::test),
            progressIndicator
        );
    }

    @Override
    public boolean processSelectedItem(
        LabelWithPreview selected,
        int modifiers,
        String searchText
    ) {
        BazelCoroutineService.getInstance(myProject).start(() ->
            // consulo: withContext(Dispatchers.EDT) { } -> the project UI access
            CompletableFuture.runAsync(
                () -> {
                    // consulo: EditorHelper.openInEditor has no focusEditor parameter
                    EditorHelper.openInEditor(selected.getElement(), true);
                },
                myProject.getUIAccess()
            )
        );
        return true;
    }

    @Override
    public ListCellRenderer<? super LabelWithPreview> getElementsRenderer() {
        return myListCellRenderer;
    }

    // consulo: abstract in Consulo, upstream uses the IntelliJ default implementation (no data)
    @Override
    public @Nullable Object getDataForItem(LabelWithPreview element, Key dataId) {
        return null;
    }

    public static class LabelWithPreview implements PsiElementNavigationItem {
        private final String myDisplayName;
        private final PsiElement myElement;

        public LabelWithPreview(String displayName, PsiElement element) {
            myDisplayName = displayName;
            myElement = element;
        }

        public String getDisplayName() {
            return myDisplayName;
        }

        public PsiElement getElement() {
            return myElement;
        }

        @Override
        public PsiElement getTargetElement() {
            return myElement;
        }

        @Override
        public @Nullable String getName() {
            return null;
        }

        @Override
        public @Nullable ItemPresentation getPresentation() {
            return null;
        }

        // consulo: abstract in Consulo, IntelliJ's Navigatable.navigate() default does nothing
        @Override
        public void navigate(boolean requestFocus) {
        }
    }

    /**
     * consulo: registered by the top-level {@link LabelSearchEverywhereContributorFactory}
     * ({@code @ExtensionImpl} on nested classes is not supported by the annotation processor).
     */
    public static class Factory implements SearchEverywhereContributorFactory<LabelWithPreview> {
        @Override
        public @Nullable SearchEverywhereContributor<LabelWithPreview> createContributor(AnActionEvent initEvent) {
            Project project = Objects.requireNonNull(initEvent.getData(Project.KEY));
            // consulo: Consulo's SearchEverywhereContributorFactory has no isAvailable(project), no contributor is created instead
            if (!isAvailable(project)) {
                return null;
            }
            return new LabelSearchEverywhereContributor(project);
        }

        public boolean isAvailable(Project project) {
            return org.jetbrains.bazel.config.project.isBazelProject(project);
        }
    }
}
