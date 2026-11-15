// Copyright 2000-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
// consulo: upstream package org.jetbrains.bazel.clion.test.catch; `catch` is a Java keyword
package org.jetbrains.bazel.clion.test.catch_;

import consulo.component.util.ModificationTracker;
import consulo.execution.action.Location;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.bazel.clion.run.BazelCcTestFrameworkSupport;
import org.jetbrains.bazel.clion.run.BazelCcTestLocationHintProvider;

import java.util.List;

// TODO consulo: not registered (upstream ccTestSupport, plugin dependency org.jetbrains.plugins.clion.test.catch),
//   CatchTestLanguageSupport / CidrCatchTestLinker / CidrCatchTestLocationProvider (com.jetbrains.cidr.execution.testing.tcatch)
//   do not exist: consulo-cpp has no Catch2 test framework support
public class CatchTestSupport implements BazelCcTestFrameworkSupport {
    // TODO consulo: CidrTestLanguageSupport / CatchTestLanguageSupport do not exist in consulo-cpp
    // override val framework: CidrTestLanguageSupport
    //   get() = CatchTestLanguageSupport.getInstance()

    @Override
    public List<Location> locate(String path, Project project, GlobalSearchScope scope) {

        String test = StringUtil.substringAfter(path, BazelCcTestLocationHintProvider.TEST_NAME_PARTS_SPLITTER);
        if (test == null) {
            test = path;
        }

        // TODO consulo: CidrCatchTestLinker / CidrCatchTestLocationProvider do not exist in consulo-cpp
        // val linker = CidrCatchTestLinker.create(test)
        //
        // return CidrCatchTestLocationProvider.INSTANCE.getLocation(
        //   CidrCatchTestLocationProvider.PROTOCOL, linker.findPath, project, scope,
        // )
        return List.of();
    }

    // TODO consulo: CidrTestScopeElement does not exist in consulo-cpp
    // /** Catch2 accepts a comma-separated list of test case names. */
    // override fun createTestFilter(tests: List<CidrTestScopeElement>): String? {
    //   return tests.map { it.pattern }.ifEmpty { null }?.joinToString(",")
    // }

    @Override
    public ModificationTracker getLocationCacheModificationTracker(Project project) {
        // TODO consulo: CidrCatchTestLocationProvider does not exist in consulo-cpp
        // CidrCatchTestLocationProvider.INSTANCE.getLocationCacheModificationTracker(project)
        return ModificationTracker.NEVER_CHANGED;
    }
}
