package org.jetbrains.bazel.golang.inspections;

import com.goide.GoLanguage;
import com.goide.psi.GoFile;
import com.goide.psi.GoImportSpec;
import com.goide.psi.GoVisitor;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.bazel.go.localize.BazelGoLocalize;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.golang.resolve.BazelGoPackageFactory;
import org.jetbrains.bazel.golang.sync.RunGazelleTargetBeforeSyncHook;
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil.GoPackageEntities;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.languages.projectview.ProjectViewExtensions;
import org.jetbrains.bazel.languages.projectview.ProjectViewService;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingProvider;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.sync.ProjectSyncScope;
import org.jetbrains.bazel.sync.ProjectSyncService;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity;
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity.ImportPathId;
import org.jspecify.annotations.Nullable;

// consulo: registration of the upstream <localInspection> (language="go", groupKey, key, enabledByDefault, level);
// upstream isDumbAware() = true -> DumbAware
@ExtensionImpl
public class BazelGoStrictDependenciesInspection extends LocalInspectionTool implements DumbAware {
    @Override
    public @Nullable Language getLanguage() {
        return GoLanguage.INSTANCE;
    }

    @Override
    public String getShortName() {
        return "BazelGoStrictDependencies";
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return BazelGoLocalize.bazelInspectionGroupName();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return BazelGoLocalize.inspectionsDisplayNameGoStrictDeps();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isAvailableForFile(PsiFile file) {
        return org.jetbrains.bazel.config.project.isBazelProject(file.getProject()) && file instanceof GoFile;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Label gazelleTarget = getGazelleTarget(holder.getProject());

        return new GoVisitor() {
            // consulo: the generated GoVisitor of consulo-google-go has no visitGoFile, files are visited by visitFile
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile psiFile) {
                if (!(psiFile instanceof GoFile file)) {
                    return;
                }
                BazelGoPackageEntity goPackageEntity = BazelGoPackageFactory.getGoPackageEntity(file);
                if (goPackageEntity == null) {
                    return;
                }
                // consulo: upstream reads the current workspace model snapshot
                GoPackageEntities currentSnapshot = GoPackageEntities.getInstance(file.getProject());
                for (GoImportSpec importSpec : file.getImports()) {
                    String importPath = importSpec.getPath();
                    // Import path is inside strict dependencies, all good
                    if (goPackageEntity.directDepsImportPaths().contains(importPath)) {
                        continue;
                    }
                    // Import path doesn't exist in the project at all, this is flagged by the Go plugin already
                    if (!currentSnapshot.contains(new ImportPathId(importPath))) {
                        continue;
                    }
                    // Import path exists in the project, but not in direct dependencies
                    holder.newProblem(
                            BazelGoLocalize.goStrictDepsIndirectDependency(
                                importPath
                            )
                        )
                        .range(importSpec)
                        .withFixes(
                            gazelleTarget != null
                                ? new LocalQuickFix[]{new RunGazelleQuickFix(file.getProject(), gazelleTarget)}
                                : LocalQuickFix.EMPTY_ARRAY
                        )
                        .create();
                }
            }
        };
    }

    private static @Nullable Label getGazelleTarget(Project project) {
        Label gazelleTarget = ProjectViewExtensions.getGazelleTarget(ProjectViewService.projectView(project));
        if (gazelleTarget != null) {
            return gazelleTarget;
        }
        boolean hasGazelle = BazelRepoMappingProvider.getCanonicalRepoNameToPath(project).keySet().stream()
            .anyMatch(it -> it.startsWith("gazelle"));
        if (hasGazelle) {
            // Use the default Gazelle location if none was specified in the project view file
            return Label.parse("//:gazelle");
        }
        else {
            return null;
        }
    }
}

class RunGazelleQuickFix implements LocalQuickFix {
    private final Project myProject;
    private final Label myGazelleTarget;

    RunGazelleQuickFix(
        Project project,
        Label gazelleTarget
    ) {
        myProject = project;
        myGazelleTarget = gazelleTarget;
    }

    @Override
    public LocalizeValue getName() {
        return BazelGoLocalize.quickfixRunGazelle(BazelRepoMappingUtils.toShortString(myGazelleTarget, myProject));
    }

    // consulo: upstream getFamilyName() = BazelGoBundle.message("quickfix.run.gazelle.family.name"); Consulo quick fixes only have
    // getName()

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        BazelCoroutineService.getInstance(project).start(() -> {
            project.putUserData(RunGazelleTargetBeforeSyncHook.GAZELLE_TARGET_OVERRIDE_KEY, myGazelleTarget);
            // consulo: Kotlin try/finally around the suspend call -> whenComplete
            return project.getInstance(ProjectSyncService.class).sync(new ProjectSyncScope.Full(false, false))
                .whenComplete((result, error) -> project.putUserData(RunGazelleTargetBeforeSyncHook.GAZELLE_TARGET_OVERRIDE_KEY, null));
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    // consulo: upstream generatePreview() = IntentionPreviewInfo.EMPTY; Consulo has no intention preview
}
