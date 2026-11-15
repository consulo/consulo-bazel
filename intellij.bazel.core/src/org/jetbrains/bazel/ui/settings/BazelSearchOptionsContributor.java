package org.jetbrains.bazel.ui.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.configurable.search.SearchableOptionContributor;
import consulo.configurable.search.SearchableOptionProcessor;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
class BazelSearchOptionsContributor extends SearchableOptionContributor {
    public BazelSearchOptionsContributor() {
    }

    @Override
    public void processOptions(SearchableOptionProcessor processor) {
        List<SettingsPage> settingPages =
            List.of(
                SettingsPages.BAZEL_PROJECT_SETTINGS_PAGE,
                SettingsPages.BAZEL_APPLICATION_SETTINGS_PAGE,
                SettingsPages.BAZEL_EXPERIMENTAL_PROJECT_SETTINGS_PAGE
            );
        for (SettingsPage page : settingPages) {
            // consulo: the page name is resolved by the index, in the locale it is built in
            LocalizeValue displayName = LocalizeValue.lazy(() -> LocalizeValue.of(page.cleanDisplayName()));
            for (String it : page.optionBundleKeys()) {
                // consulo: upstream processor.addOptions(hit, null, hit, page.id, displayName, false) - the index splits the text into words
                LocalizeValue hit = LocalizeKey.of(BazelPluginLocalize.ID, it).getValue();
                processor.addOption(hit, hit, page.id(), displayName);
            }
        }
    }

    // consulo: upstream BazelPluginBundle.message(key) with a dynamic key
    static String message(String key) {
        return LocalizeKey.of(BazelPluginLocalize.ID, key).getValue().get();
    }

    // consulo: upstream private top-level vals of BazelSearchOptionsContributor.kt, initialized on first use like the Kotlin file facade
    private static final class SettingsPages {
        private static final SettingsPage BAZEL_PROJECT_SETTINGS_PAGE =
            new SettingsPage(
                BazelProjectSettingsConfigurable.ID,
                BazelProjectSettingsConfigurable.DISPLAY_NAME_KEY,
                concat(BazelProjectSettingsConfigurable.SearchIndex.KEYS, BazelGeneralSettingsProvider.searchIndexKeys())
            );

        private static final SettingsPage BAZEL_EXPERIMENTAL_PROJECT_SETTINGS_PAGE =
            new SettingsPage(
                BazelExperimentalProjectSettingsConfigurable.ID,
                BazelExperimentalProjectSettingsConfigurable.DISPLAY_NAME_KEY,
                BazelExperimentalSettingsProvider.searchIndexKeys()
            );

        private static final SettingsPage BAZEL_APPLICATION_SETTINGS_PAGE =
            new SettingsPage(
                BazelApplicationSettingsConfigurable.ID,
                BazelApplicationSettingsConfigurable.DISPLAY_NAME_KEY,
                BazelApplicationSettingsConfigurable.SearchIndex.KEYS
            );

        private static List<String> concat(List<String> first, List<String> second) {
            List<String> result = new ArrayList<>(first);
            result.addAll(second);
            return result;
        }

        private SettingsPages() {
        }
    }
}

record SettingsPage(
    String id,
    String displayNameKey,
    List<String> optionBundleKeys
) {
    String cleanDisplayName() {
        String text = BazelSearchOptionsContributor.message(displayNameKey);
        int end = text.length();
        while (end > 0 && (Character.isWhitespace(text.charAt(end - 1)) || text.charAt(end - 1) == ':')) {
            end--;
        }
        return text.substring(0, end);
    }
}
