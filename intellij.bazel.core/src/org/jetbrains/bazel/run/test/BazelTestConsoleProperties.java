package org.jetbrains.bazel.run.test;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.component.util.ModificationTracker;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.runner.SMTestLocator;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.DumbService;
import consulo.project.Project;
import org.jetbrains.bazel.config.BazelPluginConstants;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BazelTestConsoleProperties extends SMTRunnerConsoleProperties {
    public BazelTestConsoleProperties(
        RunConfiguration config,
        Executor executor
    ) {
        super(config, BazelPluginConstants.BAZEL_DISPLAY_NAME, executor);
    }

    @Override
    public SMTestLocator getTestLocator() {
        return CombinedTestLocator.INSTANCE;
    }

    // consulo: upstream declares a public top-level interface in the same file
    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface BazelTestLocatorProvider {
        SMTestLocator getTestLocator();

        static List<SMTestLocator> testLocators() {
            List<SMTestLocator> result = new ArrayList<>();
            for (BazelTestLocatorProvider it : Application.get().getExtensionList(BazelTestLocatorProvider.class)) {
                result.add(it.getTestLocator());
            }
            return result;
        }
    }

    // consulo: upstream declares a public top-level class in the same file
    public static class CompositeModificationTracker implements ModificationTracker {
        private final List<ModificationTracker> myTrackers;

        public CompositeModificationTracker(List<ModificationTracker> trackers) {
            myTrackers = trackers;
        }

        @Override
        public long getModificationCount() {
            long sum = 0;
            for (ModificationTracker it : myTrackers) {
                sum += it.getModificationCount();
            }
            return sum;
        }
    }
}

// consulo: SMTestLocator is not DumbAware itself in Consulo, the dumb awareness of the locators is reported via PossiblyDumbAware
final class CombinedTestLocator implements SMTestLocator, PossiblyDumbAware {
    static final CombinedTestLocator INSTANCE = new CombinedTestLocator();

    private CombinedTestLocator() {
    }

    @Override
    public List<Location> getLocation(String protocol, String path, Project project, GlobalSearchScope scope) {
        return chooseFirstTestLocatorProvider(it -> it.getLocation(protocol, path, project, scope));
    }

    @Override
    public List<Location> getLocation(
        String protocol,
        String path,
        @Nullable String metainfo,
        Project project,
        GlobalSearchScope scope
    ) {
        return chooseFirstTestLocatorProvider(it -> it.getLocation(protocol, path, metainfo, project, scope));
    }

    // TODO consulo: SMTestLocator has no getLocation(stacktraceLine, project, scope)
    // override fun getLocation(stacktraceLine: String, project: Project, scope: GlobalSearchScope): List<Location<*>> =
    //   chooseFirstTestLocatorProvider { it.getLocation(stacktraceLine, project, scope) }

    private List<Location> chooseFirstTestLocatorProvider(Function<SMTestLocator, List<Location>> block) {
        for (SMTestLocator locator : BazelTestConsoleProperties.BazelTestLocatorProvider.testLocators()) {
            List<Location> locations = block.apply(locator);
            if (!locations.isEmpty()) {
                return locations;
            }
        }
        return List.of();
    }

    // TODO consulo: SMTestLocator has no getLocationCacheModificationTracker(project)
    // override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
    //   CompositeModificationTracker(BazelTestLocatorProvider.testLocators().map { it.getLocationCacheModificationTracker(project) })

    @Override
    public boolean isDumbAware() {
        for (SMTestLocator it : BazelTestConsoleProperties.BazelTestLocatorProvider.testLocators()) {
            if (!DumbService.isDumbAware(it)) {
                return false;
            }
        }
        return true;
    }
}
