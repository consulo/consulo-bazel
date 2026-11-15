package org.jetbrains.bazel.languages.starlark.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.bazel.languages.localize.StarlarkLocalize;
import consulo.component.util.ModificationTracker;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.ProjectRootModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage;
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadEdgesIndex;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.languages.starlark.utils.GraphUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public class StarlarkLoadCycleInspection extends LocalInspectionTool {
    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return file.getFileType() instanceof StarlarkLanguage.StarlarkFileType;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!(holder.getFile() instanceof StarlarkFile file)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        if (!shouldCheckLoadCycles(file)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        VirtualFile sourceFile = file.getVirtualFile();
        if (sourceFile == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
        Set<String> cyclicLoadLabels = StarlarkLoadGraph.getOrBuild(file.getProject()).cyclicLoadLabels(sourceFile);

        return cyclicLoadLabels.isEmpty() ? PsiElementVisitor.EMPTY_VISITOR : new LoadCycleVisitor(holder, cyclicLoadLabels);
    }

    private boolean shouldCheckLoadCycles(StarlarkFile receiver) {
        return receiver.getVirtualFile() != null &&
            receiver.getName().endsWith(".bzl") &&
            PsiTreeUtil.findChildOfType(receiver, StarlarkLoadStatement.class) != null;
    }

    private static class LoadCycleVisitor extends StarlarkElementVisitor {
        private final ProblemsHolder myHolder;
        private final Set<String> myCyclicLoadLabels;

        LoadCycleVisitor(ProblemsHolder holder, Set<String> cyclicLoadLabels) {
            myHolder = holder;
            myCyclicLoadLabels = cyclicLoadLabels;
        }

        @Override
        public void visitLoadStatement(StarlarkLoadStatement node) {
            StarlarkStringLiteralExpression loadLabel = node.getLoadedFileNamePsi();
            if (loadLabel == null) {
                return;
            }
            if (!myCyclicLoadLabels.contains(loadLabel.getStringContents())) {
                return;
            }

            myHolder.newProblem(StarlarkLocalize.inspectionDescriptionLoadCycle())
                .range(loadLabel)
                .create();
        }
    }

    private static class StarlarkLoadGraph {
        private final Map<VirtualFile, Map<String, VirtualFile>> myResolvedLoadsByFile;
        private final Map<VirtualFile, Integer> myCyclicComponentByFile;

        private StarlarkLoadGraph(
            Map<VirtualFile, Map<String, VirtualFile>> resolvedLoadsByFile,
            Map<VirtualFile, Integer> cyclicComponentByFile
        ) {
            myResolvedLoadsByFile = resolvedLoadsByFile;
            myCyclicComponentByFile = cyclicComponentByFile;
        }

        public Set<String> cyclicLoadLabels(VirtualFile sourceFile) {
            Integer sourceComponent = myCyclicComponentByFile.get(sourceFile);
            if (sourceComponent == null) {
                return Set.of();
            }
            Map<String, VirtualFile> resolvedLoads = myResolvedLoadsByFile.get(sourceFile);
            if (resolvedLoads == null) {
                return Set.of();
            }

            Set<String> result = new LinkedHashSet<>();
            for (Map.Entry<String, VirtualFile> entry : resolvedLoads.entrySet()) {
                if (Objects.equals(myCyclicComponentByFile.get(entry.getValue()), sourceComponent)) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }

        public static StarlarkLoadGraph getOrBuild(Project project) {
            return CachedValuesManager.getManager(project).getCachedValue(project, () ->
                CachedValueProvider.Result.create(
                    build(project),
                    (ModificationTracker) () -> FileBasedIndex.getInstance().getIndexModificationStamp(StarlarkLoadEdgesIndex.NAME, project),
                    ProjectRootModificationTracker.getInstance(project),
                    VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS
                )
            );
        }

        private static StarlarkLoadGraph build(Project project) {
            Map<VirtualFile, Map<String, VirtualFile>> resolvedLoadsByFile = collectResolvedLoadsByFile(project);
            Map<VirtualFile, Set<VirtualFile>> loadedFilesByFile = new LinkedHashMap<>();
            for (Map.Entry<VirtualFile, Map<String, VirtualFile>> entry : resolvedLoadsByFile.entrySet()) {
                loadedFilesByFile.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().values()));
            }
            Map<VirtualFile, Integer> cyclicComponentByFile = findCyclicComponentByFile(allFiles(loadedFilesByFile), loadedFilesByFile);

            return new StarlarkLoadGraph(resolvedLoadsByFile, cyclicComponentByFile);
        }

        private static Set<VirtualFile> allFiles(Map<VirtualFile, Set<VirtualFile>> receiver) {
            Set<VirtualFile> result = new LinkedHashSet<>();
            result.addAll(receiver.keySet());
            receiver.values().forEach(result::addAll);
            return result;
        }

        private static Map<VirtualFile, Map<String, VirtualFile>> collectResolvedLoadsByFile(Project project) {
            FileBasedIndex index = FileBasedIndex.getInstance();
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            Map<VirtualFile, Map<String, VirtualFile>> resolvedLoadsByFile = new LinkedHashMap<>();

            index.processAllKeys(
                StarlarkLoadEdgesIndex.NAME,
                sourceUrl -> {
                    VirtualFile sourceFile = project.getApplication().getInstance(VirtualFileManager.class).findFileByUrl(sourceUrl);
                    if (sourceFile == null) {
                        return true;
                    }
                    if (!sourceFile.isValid()) {
                        return true;
                    }
                    if (!scope.contains(sourceFile)) {
                        return true;
                    }

                    index.processValues(
                        StarlarkLoadEdgesIndex.NAME,
                        sourceUrl,
                        null,
                        (file, loadLabels) -> {
                            for (String loadLabel : loadLabels) {
                                VirtualFile loadedFile = resolveLoadLabel(project, sourceFile, loadLabel);
                                if (loadedFile == null) {
                                    continue;
                                }
                                if (!loadedFile.isValid()) {
                                    continue;
                                }
                                resolvedLoadsByFile.computeIfAbsent(sourceFile, it -> new LinkedHashMap<>()).putIfAbsent(loadLabel, loadedFile);
                            }
                            return true;
                        },
                        scope
                    );

                    return true;
                },
                project
            );

            Map<VirtualFile, Map<String, VirtualFile>> result = new LinkedHashMap<>();
            for (Map.Entry<VirtualFile, Map<String, VirtualFile>> entry : resolvedLoadsByFile.entrySet()) {
                result.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
            return result;
        }

        private static @Nullable VirtualFile resolveLoadLabel(Project project, VirtualFile sourceFile, String loadLabel) {
            Label label = Label.parseOrNull(loadLabel);
            if (label == null) {
                return null;
            }
            return LabelResolveUtils.resolveFileTargetToVirtualFile(project, label, sourceFile);
        }

        private static Map<VirtualFile, Integer> findCyclicComponentByFile(
            Collection<VirtualFile> files,
            Map<VirtualFile, Set<VirtualFile>> loadedFilesByFile
        ) {
            List<List<VirtualFile>> components = GraphUtils.findCyclicStronglyConnectedComponents(
                files,
                it -> loadedFilesByFile.getOrDefault(it, Set.of())
            );
            Map<VirtualFile, Integer> result = new LinkedHashMap<>();
            for (int index = 0; index < components.size(); index++) {
                for (VirtualFile it : components.get(index)) {
                    result.put(it, index);
                }
            }
            return result;
        }
    }

    // consulo: registration attributes of <localInspection> in intellij.bazel.core.xml
    @Override
    public @Nullable Language getLanguage() {
        return StarlarkLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "StarlarkLoadCycle";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelPluginLocalize.inspectionDisplayNameStarlarkLoadCycleCheck();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelPluginLocalize.starlarkInspectionGroupName();
    }

    // consulo: upstream groupPath="Bazel"
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{BazelPluginLocalize.bazelInspectionGroupName(), getGroupDisplayName()};
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
