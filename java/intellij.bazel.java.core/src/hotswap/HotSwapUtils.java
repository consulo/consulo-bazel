/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.hotswap;

import consulo.project.Project;
import org.jetbrains.bazel.config.BazelFeatureFlags;
import org.jetbrains.bazel.settings.bazel.BazelJVMProjectSettings;

/**
 * Helper methods for HotSwapping
 */
public final class HotSwapUtils {
    private HotSwapUtils() {
    }

    public static boolean isHotSwapEligible(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project) &&
            BazelJVMProjectSettings.getBazelJVMProjectSettings(project).hotSwapEnabled() &&
            !BazelFeatureFlags.getFastBuildEnabled();
    }
}
