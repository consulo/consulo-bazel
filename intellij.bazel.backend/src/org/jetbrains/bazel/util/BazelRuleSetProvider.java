package org.jetbrains.bazel.util;

import com.intellij.aspect.lib.Rules;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import org.jetbrains.bazel.commons.BazelInfo.BazelRelease;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelRuleSetProvider {
    // consulo: upstream top-level declarations BazelRuleSet and isBundledFor of BazelRuleSetProvider.kt are nested here

    record BazelRuleSet(
        Rules aspectLanguage,
        List<String> rulesetNames,
        /*
         * Whether Bazel exposes this language natively before the Bazel 8 external rules migration.
         *
         * For Bazel 8 and later, bundled languages are only enabled when this language has explicit
         * autoload hints and Bazel reports matching external autoloads, which preserves the external-rules
         * path for languages such as protobuf.
         */
        boolean isBundled,
        List<String> autoloadHints,
        List<String> hostLocations,
        @Nullable String deprecated
    ) {
        // consulo: overloads for the upstream default arguments
        public BazelRuleSet(Rules aspectLanguage, List<String> rulesetNames, boolean isBundled) {
            this(aspectLanguage, rulesetNames, isBundled, List.of());
        }

        public BazelRuleSet(Rules aspectLanguage, List<String> rulesetNames, boolean isBundled, List<String> autoloadHints) {
            this(aspectLanguage, rulesetNames, isBundled, autoloadHints, List.of());
        }

        public BazelRuleSet(
            Rules aspectLanguage,
            List<String> rulesetNames,
            boolean isBundled,
            List<String> autoloadHints,
            List<String> hostLocations
        ) {
            this(aspectLanguage, rulesetNames, isBundled, autoloadHints, hostLocations, null);
        }
    }

    static boolean isBundledFor(BazelRuleSet receiver, BazelRelease bazelRelease, List<String> externalAutoloads) {
        if (!receiver.isBundled()) {
            return false;
        }
        if (bazelRelease.major() < 8) {
            return true;
        }
        // Bazel 8+ only restores the bundled path for languages that opt in via [autoloadHints].
        // Languages without [autoloadHints] (e.g. Protobuf) must instead be discovered as an
        // external ruleset by name. Match against both [rulesetNames] and [autoloadHints]
        // because --incompatible_autoload_externally accepts ruleset names (e.g. "rules_java")
        // as well as individual symbol names (e.g. "JavaInfo", "PyInfo").
        if (receiver.autoloadHints().isEmpty()) {
            return false;
        }
        return Stream.concat(receiver.rulesetNames().stream(), receiver.autoloadHints().stream()).anyMatch(externalAutoloads::contains);
    }

    List<BazelRuleSet> ruleSets();

    static ExtensionPoint<BazelRuleSetProvider> getEp() {
        return Application.get().getExtensionPoint(BazelRuleSetProvider.class);
    }

    static List<BazelRuleSet> all() {
        List<BazelRuleSet> result = new ArrayList<>();
        getEp().forEachExtensionSafe(provider -> {
            result.addAll(provider.ruleSets());
        });
        return result;
    }
}
