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

package org.jetbrains.bazel.clion.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.util.ModificationTracker;
import consulo.execution.action.Location;
import consulo.execution.test.sm.runner.SMTestLocator;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties.BazelTestLocatorProvider;
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties.CompositeModificationTracker;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the generic {@link BazelCcTestLocationHintProvider#BAZEL_CC_PROTOCOL} location URLs asking every registered
 * {@link BazelCcTestFrameworkSupport} in turn.
 */
final class BazelCcTestLocator implements SMTestLocator, DumbAware {
    static final BazelCcTestLocator INSTANCE = new BazelCcTestLocator();

    private BazelCcTestLocator() {
    }

    @Override
    public List<Location> getLocation(
        String protocol,
        String path,
        Project project,
        GlobalSearchScope scope
    ) {
        if (!protocol.equals(BazelCcTestLocationHintProvider.BAZEL_CC_PROTOCOL)) {
            return List.of();
        }

        for (BazelCcTestFrameworkSupport it : BazelCcTestFrameworkSupport.getExtensions()) {
            List<Location> locations = navigable(it.locate(path, project, scope));
            if (locations != null) {
                return locations;
            }
        }
        return List.of();
    }

    /**
     * Discards results which resolved only to a non-navigable re-run placeholder.
     */
    private @Nullable List<Location> navigable(List<Location> locations) {
        for (Location it : locations) {
            if (hasFile(it)) {
                return locations;
            }
        }
        return null;
    }

    private boolean hasFile(Location location) {
        return location.getVirtualFile() != null;
    }

    // consulo: SMTestLocator has no getLocationCacheModificationTracker(project), the method is kept without override
    public ModificationTracker getLocationCacheModificationTracker(Project project) {
        List<ModificationTracker> trackers = new ArrayList<>();
        for (BazelCcTestFrameworkSupport it : BazelCcTestFrameworkSupport.getExtensions()) {
            trackers.add(it.getLocationCacheModificationTracker(project));
        }
        return new CompositeModificationTracker(trackers);
    }
}

@ExtensionImpl
class BazelCcTestLocatorProvider implements BazelTestLocatorProvider {
    public BazelCcTestLocatorProvider() {
    }

    @Override
    public SMTestLocator getTestLocator() {
        return BazelCcTestLocator.INSTANCE;
    }
}
