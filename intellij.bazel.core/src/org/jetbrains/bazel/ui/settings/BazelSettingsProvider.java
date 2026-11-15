package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;

import java.util.List;

// consulo: upstream registers this interface as two extension points (bazelExperimentalSettingsProvider and
// bazelGeneralSettingsProvider); Consulo has one extension point per interface, isExperimental() selects the list
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelSettingsProvider {
    UnnamedConfigurable createConfigurable(Project project);

    List<String> searchIndexKeys();

    /**
     * consulo: {@code true} - upstream extension point {@code org.jetbrains.bazel.bazelExperimentalSettingsProvider},
     * {@code false} - upstream extension point {@code org.jetbrains.bazel.bazelGeneralSettingsProvider}
     */
    default boolean isExperimental() {
        return false;
    }

    static List<BazelSettingsProvider> getBazelExperimentalSettingsProviders(Project receiver) {
        return BazelExperimentalSettingsProvider.getExtensionList();
    }

    static List<BazelSettingsProvider> getBazelGeneralSettingsProviders(Project receiver) {
        return BazelGeneralSettingsProvider.getExtensionList();
    }
}

final class BazelExperimentalSettingsProvider {
    private BazelExperimentalSettingsProvider() {
    }

    // consulo: upstream ep.extensionList
    static List<BazelSettingsProvider> getExtensionList() {
        return Application.get().getExtensionList(BazelSettingsProvider.class).stream()
            .filter(BazelSettingsProvider::isExperimental)
            .toList();
    }

    static List<String> searchIndexKeys() {
        return getExtensionList().stream().flatMap(it -> it.searchIndexKeys().stream()).toList();
    }
}

final class BazelGeneralSettingsProvider {
    private BazelGeneralSettingsProvider() {
    }

    // consulo: upstream ep.extensionList
    static List<BazelSettingsProvider> getExtensionList() {
        return Application.get().getExtensionList(BazelSettingsProvider.class).stream()
            .filter(it -> !it.isExperimental())
            .toList();
    }

    static List<String> searchIndexKeys() {
        return getExtensionList().stream().flatMap(it -> it.searchIndexKeys().stream()).toList();
    }
}
