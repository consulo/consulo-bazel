package org.jetbrains.bazel.flow.modify;

import com.intellij.java.impl.openapi.roots.JavaProjectModelModifier;
import com.intellij.java.impl.openapi.roots.impl.IdeaProjectModelModifier;
import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.projectRoots.roots.ExternalLibraryDescriptor;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.content.library.Library;
import consulo.language.ast.ASTNode;
import consulo.language.editor.WriteCommandAction;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.bazel.workspace.BazelProjectModelService;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.project.Project;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Inject;
import org.jetbrains.bazel.coroutines.BazelCoroutineService;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.label.Label.ResolvedLabel;
import org.jetbrains.bazel.languages.starlark.formatting.StarlarkFormattingService;
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression;
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList;
import org.jetbrains.bazel.languages.starlark.references.LabelResolveUtils;
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator;
import org.jetbrains.bazel.languages.starlark.repomapping.BazelRepoMappingUtils;
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier;
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction;
import org.jetbrains.bazel.workspacemodel.entities.extensions;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelLibraryExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelModuleExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetLabelList;
import org.jetbrains.bsp.protocol.BuildTarget.StrictDependencyCheckedType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// consulo: Promise<Void> -> AsyncResult<Void> (Consulo JavaProjectModelModifier API, no `exported` parameter);
//   suspend functions -> CompletableFuture; readAction { } -> ReadAction.compute on the coroutine thread;
//   WriteCommandAction.runWriteCommandAction is started on the UI thread with UIAccess.giveAsync.
//   upstream IdeaProjectModelModifier is registered with order="last"; in Consulo it has no order and order "first"
//   is restricted to the plugin owning the extension point, so the modifier is ordered before it
@ExtensionImpl(order = "before IdeaProjectModelModifier")
public class BazelProjectModelModifier extends JavaProjectModelModifier {
    private static final Logger LOG = Logger.getInstance(BazelProjectModelModifier.class);

    private final Project myProject;
    private final IdeaProjectModelModifier myIdeaProjectModelModifier;

    @Inject
    public BazelProjectModelModifier(Project project) {
        myProject = project;
        myIdeaProjectModelModifier = new IdeaProjectModelModifier(project);
    }

    // consulo: upstream addModuleDependency(from, to, scope, exported)
    @Override
    public @Nullable AsyncResult<Void> addModuleDependency(
        Module from,
        Module to,
        DependencyScope scope
    ) {
        return asyncPromise(() -> {
            BazelModuleExtensionEntity toExtension = extensions.getBazelModuleExtension(to);
            Label labelToInsert = toExtension != null ? toExtension.getTargetKey().label() : null;
            return tryAddingModuleDependencyToBuildFile(from, labelToInsert).thenCompose(added -> {
                if (added) {
                    // We used to do a partial resync here, but simply modifying the project model is quicker
                    addToStrictDependenciesList(from, labelToInsert);
                    // consulo: upstream passes exported = true, the Consulo API has no `exported` parameter
                    return await(() -> myIdeaProjectModelModifier.addModuleDependency(from, to, scope));
                }
                else {
                    return jumpToBuildFile(from).thenRun(this::notifyAutomaticDependencyAdditionFailure);
                }
            });
        });
    }

    // consulo: upstream addLibraryDependency(from, library, scope, exported)
    @Override
    public @Nullable AsyncResult<Void> addLibraryDependency(
        Module from,
        Library library,
        DependencyScope scope
    ) {
        return asyncPromise(() -> {
            BazelLibraryExtensionEntity libraryExtension = extensions.getBazelLibraryExtension(from.getProject(), library);
            Label labelToInsert = libraryExtension != null && !libraryExtension.isSynthetic()
                ? libraryExtension.getTargetKey().label()
                : null;
            return tryAddingModuleDependencyToBuildFile(from, labelToInsert).thenCompose(added -> {
                if (added) {
                    addToStrictDependenciesList(from, labelToInsert);
                    // We should actually depend on the library module, not the library itself
                    String libraryModuleName = library.getName();
                    if (libraryModuleName == null) {
                        throw new IllegalStateException("Required value was null.");
                    }
                    Module libraryModule = ReadAction.<@Nullable Module, RuntimeException>compute(
                        () -> ModuleManager.getInstance(myProject).findModuleByName(libraryModuleName)
                    );
                    if (libraryModule != null) {
                        // consulo: upstream passes exported = true, the Consulo API has no `exported` parameter
                        return await(() -> myIdeaProjectModelModifier.addModuleDependency(from, libraryModule, scope));
                    }
                    else {
                        LOG.warn("Can't find library module " + libraryModuleName);
                    }
                    return await(() -> myIdeaProjectModelModifier.addLibraryDependency(from, library, scope));
                }
                else {
                    return jumpToBuildFile(from).thenRun(this::notifyAutomaticDependencyAdditionFailure);
                }
            });
        });
    }

    private CompletableFuture<Boolean> tryAddingModuleDependencyToBuildFile(Module from, @Nullable Label labelToInsert) {
        if (!(labelToInsert instanceof ResolvedLabel resolvedLabelToInsert)) {
            return CompletableFuture.completedFuture(false);
        }
        BazelModuleExtensionEntity fromExtension = extensions.getBazelModuleExtension(from);
        if (fromExtension == null) {
            return CompletableFuture.completedFuture(false);
        }
        ResolvedLabel targetRuleLabel = Label.assumeResolved(fromExtension.getTargetKey().label());
        StarlarkFile targetBuildFile = ReadAction.<@Nullable StarlarkFile, RuntimeException>compute(
            () -> LabelResolveUtils.findBuildFile(from.getProject(), targetRuleLabel)
        );
        if (targetBuildFile == null) {
            return CompletableFuture.completedFuture(false);
        }
        StarlarkCallExpression ruleTarget = ReadAction.<@Nullable StarlarkCallExpression, RuntimeException>compute(
            () -> targetBuildFile.findTargetRule(targetRuleLabel.getTargetName())
        );
        if (ruleTarget == null) {
            return CompletableFuture.completedFuture(false);
        }
        StarlarkArgumentList argList = ReadAction.<@Nullable StarlarkArgumentList, RuntimeException>compute(
            () -> ruleTarget.getArgumentList()
        );
        if (argList == null) {
            return CompletableFuture.completedFuture(false);
        }
        StarlarkNamedArgumentExpression depsArg = ReadAction.<@Nullable StarlarkNamedArgumentExpression, RuntimeException>compute(
            () -> argList.getDepsArgument()
        );
        CompletableFuture<?> depsArgCreated;
        if (depsArg == null) {
            depsArgCreated = myProject.getUIAccess().giveAsync(() -> {
                WriteCommandAction.runWriteCommandAction(from.getProject(), () -> {
                    // Create a new deps argument with an empty list
                    String emptyDepsList = "deps = [],";
                    ASTNode node = new StarlarkElementGenerator(myProject).createNamedArgument(emptyDepsList);
                    argList.addBefore(node.getPsi(), argList.getLastChild());
                });
            });
        }
        else {
            depsArgCreated = CompletableFuture.completedFuture(null);
        }
        return depsArgCreated.thenCompose(ignored -> {
            // After creating the deps argument, we need to re-fetch it
            StarlarkNamedArgumentExpression updatedDepsArg = depsArg != null
                ? depsArg
                : ReadAction.<@Nullable StarlarkNamedArgumentExpression, RuntimeException>compute(() -> argList.getDepsArgument());
            StarlarkListLiteralExpression depsList =
                ReadAction.<@Nullable StarlarkListLiteralExpression, RuntimeException>compute(() -> {
                    if (updatedDepsArg == null) {
                        return null;
                    }
                    return Arrays.stream(updatedDepsArg.getChildren())
                        .filter(it -> it instanceof StarlarkListLiteralExpression)
                        .map(it -> (StarlarkListLiteralExpression) it)
                        .findFirst()
                        .orElse(null);
                });

            return myProject.getUIAccess().giveAsync(() -> {
                boolean[] insertSuccessful = {false};

                try {
                    WriteCommandAction.runWriteCommandAction(from.getProject(), () -> {
                        if (depsList != null) {
                            depsList.insertString(BazelRepoMappingUtils.toShortString(resolvedLabelToInsert, myProject));
                        }
                        insertSuccessful[0] = true;
                    });
                }
                catch (Exception e) {
                    LOG.warn("Failed to insert target " + resolvedLabelToInsert + " as a dependency for target " + targetRuleLabel, e);
                }
                if (insertSuccessful[0]) {
                    // consulo: formatBuildFile runs in the caller's write action (upstream suspend readAndWriteAction)
                    BazelCoroutineService.getInstance(from.getProject()).start(() ->
                        myProject.getUIAccess().giveAsync(() -> {
                            WriteAction.run(() -> StarlarkFormattingService.formatBuildFile(targetBuildFile));
                        })
                    );
                }
                return insertSuccessful[0];
            });
        });
    }

    private void addToStrictDependenciesList(Module from, @Nullable Label labelToInsert) {
        if (labelToInsert == null) {
            return;
        }

        BazelModuleExtensionEntity bazelModuleExt = extensions.getBazelModuleExtension(from);
        if (bazelModuleExt == null) {
            return;
        }
        if (bazelModuleExt.strictDependencies().check() == StrictDependencyCheckedType.OFF) {
            return; // Module does not use strict deps
        }

        // consulo: WorkspaceModel.update("Add $labelToInsert to strict dependencies list") { modifyModuleEntity(...) } ->
        //   a new BazelModuleExtensionEntity record for the module data
        List<String> labels = new ArrayList<>(bazelModuleExt.strictDependencies().labels());
        labels.add(labelToInsert.toString());
        BazelModuleExtensionEntity newBazelModuleExt = new BazelModuleExtensionEntity(
            /* _targetKey = */ bazelModuleExt._targetKey(),
            /* rootTypeId = */ bazelModuleExt.rootTypeId(),
            /* strictDependencies = */ new WorkspaceModelTargetLabelList(
                bazelModuleExt.strictDependencies().check(),
                List.copyOf(labels)
            )
        );
        BazelProjectModelService.getInstance(myProject).putModuleData(from, BazelModuleExtensionEntity.class, newBazelModuleExt);
    }

    private void notifyAutomaticDependencyAdditionFailure() {
        BazelBalloonNotifier.warn(
            BazelPluginLocalize.balloonAddTargetDependencyToBuildFileFailedTitle(),
            BazelPluginLocalize.balloonAddTargetDependencyToBuildFileFailedMessage()
        );
    }

    @Override
    public @Nullable AsyncResult<Void> addExternalLibraryDependency(
        Collection<Module> modules,
        ExternalLibraryDescriptor descriptor,
        DependencyScope scope
    ) {
        return asyncPromise(() -> {
            if (modules.size() != 1) {
                Module first = modules.isEmpty() ? null : modules.iterator().next();
                CompletableFuture<Void> jump = first != null ? jumpToBuildFile(first) : CompletableFuture.<Void>completedFuture(null);
                return jump.thenRun(this::notifyAutomaticDependencyAdditionFailure);
            }

            // TODO consulo: Library.getMavenCoordinates() (com.intellij.java.library) is missing in Consulo
            //val libraryToAdd =
            //  LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries.firstOrNull {
            //    val mavenCoordinates = it.getMavenCoordinates() ?: return@firstOrNull false
            //    mavenCoordinates.groupId == descriptor.libraryGroupId && mavenCoordinates.artifactId == descriptor.libraryArtifactId
            //  }
            @Nullable Library libraryToAdd = null;
            if (libraryToAdd != null) {
                AsyncResult<Void> result = addLibraryDependency(modules.iterator().next(), libraryToAdd, scope);
                return result != null ? result.toCompletableFuture() : CompletableFuture.<Void>completedFuture(null);
            }

            return jumpToBuildFile(modules.iterator().next()).thenRun(this::notifyAutomaticDependencyAdditionFailure);
        });
    }

    @Override
    public @Nullable AsyncResult<Void> changeLanguageLevel(Module module, LanguageLevel level) {
        return asyncPromise(() -> jumpToBuildFile(module));
    }

    private CompletableFuture<Void> jumpToBuildFile(Module receiver) {
        BazelModuleExtensionEntity extension = extensions.getBazelModuleExtension(receiver);
        if (extension == null) {
            return CompletableFuture.<Void>completedFuture(null);
        }
        Label target = extension.getTargetKey().label();
        return BazelJumpToBuildFileAction.jumpToBuildFile(myProject, target);
    }

    // consulo: upstream `ideaProjectModelModifier.xxx(...)?.await()`; the IDEA modifier commits root models, so it is called
    //   on the UI thread
    private CompletableFuture<Void> await(Supplier<@Nullable AsyncResult<Void>> call) {
        return myProject.getUIAccess()
            .giveAsync(call::get)
            .thenCompose(result -> result != null ? result.toCompletableFuture() : CompletableFuture.<Void>completedFuture(null));
    }

    private @Nullable AsyncResult<Void> asyncPromise(Supplier<CompletableFuture<Void>> callable) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(myProject)) {
            return null;
        }
        AsyncResult<Void> promise = new AsyncResult<>();
        BazelCoroutineService.getInstance(myProject).startAsync(callable).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                promise.rejectWithThrowable(throwable);
            }
            else {
                promise.setDone(null);
            }
        });
        return promise;
    }
}
