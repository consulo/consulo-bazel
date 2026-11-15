// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.clion.run;

import consulo.execution.test.AbstractTestProxy;
import org.jetbrains.bazel.run.BazelTestRerunExtension.BazelTestFilterRerunExtension;
import org.jetbrains.bazel.run.config.BazelRunConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.List;

// TODO consulo: not registered (upstream testRerunExtension), BazelCcTestFrameworkSupport.findTestAndFramework and
//   createTestFilter need CidrTestScopeElement (com.jetbrains.cidr.execution.testing), which does not exist in consulo-cpp
public class BazelCcTestRerunExtension extends BazelTestFilterRerunExtension {
    @Override
    public boolean isApplicable(BazelRunConfiguration configuration) {
        return configuration.getHandler() instanceof CcRunHandler.CcTestHandler;
    }

    @Override
    public @Nullable String getTestFilter(BazelRunConfiguration configuration, List<AbstractTestProxy> tests) {
        // TODO consulo: CidrTestScopeElement does not exist in consulo-cpp
        // val project = configuration.project
        // val testLocations = tests.mapNotNull { it.getLocation(project, GlobalSearchScope.allScope(project)) }
        // val testsAndFrameworks = testLocations.mapNotNull { BazelCcTestFrameworkSupport.findTestAndFramework(it.psiElement) }
        // // a single Bazel invocation carries one --test_filter, which is framework-specific
        // val frameworkSupport = testsAndFrameworks.map { it.second }.distinct().singleOrNull() ?: return null
        // return frameworkSupport.createTestFilter(testsAndFrameworks.map { it.first })
        return null;
    }
}
