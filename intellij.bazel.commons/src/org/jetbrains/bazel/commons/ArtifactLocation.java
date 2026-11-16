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
package org.jetbrains.bazel.commons;

import java.nio.file.Paths;
import java.util.Comparator;

/** Represents a blaze-produced artifact.  */
class ArtifactLocation implements Comparable<ArtifactLocation> {
    private final String myRootExecutionPathFragment;
    private final String myRelativePath;
    private final boolean myIsSource;
    private final boolean myIsExternal;

    private ArtifactLocation(
        String rootExecutionPathFragment,
        String relativePath,
        boolean isSource,
        boolean isExternal
    ) {
        myRootExecutionPathFragment = rootExecutionPathFragment;
        myRelativePath = relativePath;
        myIsSource = isSource;
        myIsExternal = isExternal;
    }

    public String getRootExecutionPathFragment() {
        return myRootExecutionPathFragment;
    }

    /**
     * The root-relative path. For external workspace artifacts, this is relative to the external
     * workspace root.
     */
    public String getRelativePath() {
        return myRelativePath;
    }

    public boolean isSource() {
        return myIsSource;
    }

    public boolean isExternal() {
        return myIsExternal;
    }

    public boolean isGenerated() {
        return !this.isSource();
    }

    /** Returns false for generated or external artifacts  */
    public boolean isMainWorkspaceSourceArtifact() {
        return this.isSource() && !this.isExternal();
    }

    /** For main-workspace source artifacts, this is simply the workspace-relative path.  */
    public String getExecutionRootRelativePath() {
        return Paths.get(this.getRootExecutionPathFragment(), this.getRelativePath()).toString();
    }

    @Override
    public String toString() {
        return this.getExecutionRootRelativePath();
    }

    @Override
    public int compareTo(ArtifactLocation o) {
        return Comparator.comparing(ArtifactLocation::getRootExecutionPathFragment)
            .thenComparing(ArtifactLocation::getRelativePath)
            .thenComparing(ArtifactLocation::isSource)
            .thenComparing(ArtifactLocation::isExternal)
            .compare(this, o);
    }
}
