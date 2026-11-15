// Copyright 2000-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.clion.test.google;

import consulo.component.util.ModificationTracker;
import consulo.execution.action.Location;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.clion.run.BazelCcTestFrameworkSupport;
import org.jetbrains.bazel.clion.run.BazelCcTestLocationHintProvider;

import java.util.List;

// TODO consulo: not registered (upstream ccTestSupport, plugin dependency org.jetbrains.plugins.clion.test.google),
//   GoogleTestLanguageSupport / CidrGoogleTestLinker / CidrGoogleTestLocationProvider (com.jetbrains.cidr.execution.testing.google)
//   do not exist: consulo-cpp has no Google Test framework support
public class GoogleTestSupport implements BazelCcTestFrameworkSupport {
    // TODO consulo: CidrTestLanguageSupport / GoogleTestLanguageSupport do not exist in consulo-cpp
    // override val framework: CidrTestLanguageSupport
    //   get() = GoogleTestLanguageSupport.getInstance()

    @Override
    public List<Location> locate(String path, Project project, GlobalSearchScope scope) {
        String suiteOrPath = StringUtil.substringBefore(path, BazelCcTestLocationHintProvider.TEST_NAME_PARTS_SPLITTER);
        if (suiteOrPath == null) {
            suiteOrPath = path;
        }
        String suite = suiteOrPath.isEmpty() ? null : suiteOrPath;
        String test = StringUtil.substringAfter(path, BazelCcTestLocationHintProvider.TEST_NAME_PARTS_SPLITTER);
        if (test == null) {
            test = path;
        }
        String testOut = suite + "." + test;

        // TODO consulo: CidrGoogleTestLinker / CidrGoogleTestLocationProvider do not exist in consulo-cpp
        // val linker = CidrGoogleTestLinker.parseTestOut("$suite.$test") ?: return emptyList()
        // return CidrGoogleTestLocationProvider.INSTANCE.getLocation(
        //   CidrGoogleTestLocationProvider.PROTOCOL, linker.path, project, scope,
        // )
        return List.of();
    }

    // TODO consulo: CidrTestScopeElement does not exist in consulo-cpp
    // /** Google Test accepts a colon-separated list of patterns. */
    // override fun createTestFilter(tests: List<CidrTestScopeElement>): String? {
    //   if (tests.isEmpty()) {
    //     return null
    //   }
    //
    //   return tests.joinToString(":") { it.pattern }
    // }

    @Override
    public ModificationTracker getLocationCacheModificationTracker(Project project) {
        // TODO consulo: CidrGoogleTestLocationProvider does not exist in consulo-cpp
        // CidrGoogleTestLocationProvider.INSTANCE.getLocationCacheModificationTracker(project)
        return ModificationTracker.NEVER_CHANGED;
    }
}
