/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.sync.workspace.targetKind;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bazel.commons.TargetKind;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * We cache target kinds provided by the extension points above. This state is associated with an
 * application, so for example needs to be reset between unit test runs with manual extension
 * point setup.
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class TargetKindService {
    /**
     * Provides a set of recognized bazel rule names. Individual language-specific sub-plugins can use
     * this EP to register rule types relevant to that language.
     * <p>
     * <p>
     * Each rule name must map to at most one Kind, across all such providers.
     */
    // consulo: upstream declares this interface as a top-level declaration in the same file
    @ExtensionAPI(ComponentScope.APPLICATION)
    public interface TargetKindProvider {
        /**
         * A set of rule names known at compile time.
         */
        Set<TargetKind> getTargetKinds();

        static ExtensionPoint<TargetKindProvider> getEp() {
            return Application.get().getExtensionPoint(TargetKindProvider.class);
        }
    }

    /**
     * An internal map of all known rule types.
     */
    private final Map<String, TargetKind> myStringToKind;

    @Inject
    public TargetKindService() {
        Map<String, TargetKind> stringToKind = new LinkedHashMap<>();
        for (TargetKindProvider provider : TargetKindProvider.getEp().getExtensionList()) {
            for (TargetKind targetKind : provider.getTargetKinds()) {
                stringToKind.put(targetKind.kind(), targetKind);
            }
        }
        myStringToKind = stringToKind;
    }

    public @Nullable TargetKind findPredefinedRule(String ruleName) {
        String name = ruleName;
        if (name.startsWith(RULE_NAME_PREFIX_TRANSITION)) {
            name = name.substring(RULE_NAME_PREFIX_TRANSITION.length());
        }
        return myStringToKind.get(name);
    }

    public TargetKind guessFromRuleName(String ruleName) {
        TargetKind predefinedRule = findPredefinedRule(ruleName);
        return predefinedRule != null
            ? predefinedRule
            : new TargetKind(ruleName, guessRuleType(ruleName));
    }

    /**
     * If rule type isn't recognized, uses a heuristic to guess the rule type.
     */
    private RuleType guessRuleType(String ruleName) {
        TargetKind kind = findPredefinedRule(ruleName);
        if (kind != null) {
            return kind.ruleType();
        }
        else if (isTestSuite(ruleName) || ruleName.endsWith("_test")) {
            return RuleType.TEST;
        }
        else if (ruleName.endsWith("_binary")) {
            return RuleType.BINARY;
        }
        else {
            return RuleType.LIBRARY;
        }
    }

    private boolean isTestSuite(String ruleName) {
        // handle plain test_suite targets and macros producing a test/test_suite
        return ruleName.endsWith("test_suite") || ruleName.endsWith("test_suites");
    }

    public static TargetKindService getInstance() {
        return Application.get().getInstance(TargetKindService.class);
    }

    /**
     * When a rule has a transition applied to it then it will present with this
     * prefix on it.
     */
    private static final String RULE_NAME_PREFIX_TRANSITION = "_transition_";
}
