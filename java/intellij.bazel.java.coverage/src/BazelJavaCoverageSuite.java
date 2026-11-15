package org.jetbrains.bazel.java.coverage;

import com.intellij.java.coverage.JavaCoverageEngine;
import com.intellij.java.coverage.JavaCoverageSuite;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.bazel.label.Label;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jetbrains.bazel.target.ModuleTargetService;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BazelJavaCoverageSuite extends JavaCoverageSuite {
    // consulo: the Consulo JavaCoverageSuite takes a JavaCoverageEngine
    public BazelJavaCoverageSuite(JavaCoverageEngine coverageEngine) {
        super(coverageEngine);
        init();
    }

    // consulo: the Consulo JavaCoverageSuite has no exclude patterns and calls branchCoverage "tracingEnabled"
    public BazelJavaCoverageSuite(
        @Nullable String name,
        @Nullable CoverageFileProvider coverageDataFileProvider,
        String @Nullable [] includeFilters,
        String @Nullable [] excludePatterns,
        long lastCoverageTimeStamp,
        boolean coverageByTestEnabled,
        boolean branchCoverage,
        boolean trackTestFolders,
        @Nullable CoverageRunner coverageRunner,
        JavaCoverageEngine coverageEngine,
        @Nullable Project project
    ) {
        super(
            name,
            coverageDataFileProvider,
            includeFilters,
            // TODO consulo: JavaCoverageSuite has no exclude patterns
            // excludePatterns,
            lastCoverageTimeStamp,
            coverageByTestEnabled,
            branchCoverage,
            trackTestFolders,
            coverageRunner,
            coverageEngine,
            project
        );
        init();
    }

    // consulo: upstream init { } block, called by both constructors
    private void init() {
        /*
         * To show uncovered classes in the coverage results, the platform needs to scan all the output jars (e.g., via OrderEnumerationHandler).
         * Not only is this slow for big monorepos, we need to make sure these jars are refreshed in the VFS (and we don't want to do it with bazel-out).
         * So while theoretically possible, it's not really worth the performance hit.
         */
        // TODO consulo: BaseCoverageSuite has no isSkipUnloadedClassesAnalysis
        // isSkipUnloadedClassesAnalysis = true
    }

    /**
     * {@code com.intellij.coverage.BaseCoverageSuite.getSearchScope} assumes a configuration is an instance of
     * {@link consulo.execution.configuration.ModuleBasedConfiguration}.
     * {@link BazelRunConfiguration} isn't, hence a custom implementation here to get the module for limiting the search scope.
     */
    // TODO consulo: BaseCoverageSuite/CoverageSuite have no getSearchScope(project), nothing calls this method yet
    public GlobalSearchScope getSearchScope(Project project) {
        RunConfigurationBase configuration = getConfiguration();
        GlobalSearchScope scope;
        if (isTrackTestFolders()) {
            scope = GlobalSearchScope.projectScope(project);
        }
        else {
            scope = GlobalSearchScopesCore.projectProductionScope(project);
        }

        Module module = null;
        if (configuration instanceof BazelRunConfiguration bazelRunConfiguration) {
            List<Label> targets = bazelRunConfiguration.getTargets();
            if (targets.size() == 1) {
                List<Module> modules =
                    project.getInstance(ModuleTargetService.class).findLegacyModulesByLabel(/* label = */ targets.get(0));
                module = modules.isEmpty() ? null : modules.get(0);
            }
        }
        if (module == null) {
            return scope;
        }
        // Follow both runtime and compile dependencies, but don't add libraries
        // consulo: upstream ModuleWithDependenciesScopeCache.getCachedScope(module, TESTS or MODULES); Consulo's
        // moduleWithDependenciesScope is the module with its module dependencies and tests, without libraries
        GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesScope(module);
        return moduleScope.intersectWith(scope);
    }
}
