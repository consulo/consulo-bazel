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

import consulo.logging.Logger;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A manifest of .class file hashes for jars needed at runtime. Used for HotSwapping.
 * The implementation assumes the source for .class file can be found exclusively from jar files.
 */
public class JarFileManifest {
    // jar file timestamps
    private final Map<Path, FileTime> myJarFileState;
    // per-jar manifest of .class file hashes
    private final Map<Path, JarManifest> myJarManifests;

    private JarFileManifest(
        Map<Path, FileTime> jarFileState,
        Map<Path, JarManifest> jarManifests
    ) {
        myJarFileState = jarFileState;
        myJarManifests = jarManifests;
    }

    public record Diff(MultiMap<Path, String> perJarModifiedFiles) {
    }

    /**
     * file manifest for a single jar.
     */
    private record JarManifest(Path jar, Map<String, Long> nameToHash) {
        static @Nullable JarManifest build(Path file) {
            try {
                JarFile jar = new JarFile(file.toFile());
                Map<String, Long> nameToHash = new LinkedHashMap<>();
                for (JarEntry entry : Collections.list(jar.entries())) {
                    if (!entry.isDirectory()) {
                        nameToHash.put(entry.getName(), entry.getCrc());
                    }
                }
                return new JarManifest(file, nameToHash);
            }
            catch (IOException e) {
                logger.warn(
                    "Error reading jar file: " + file,
                    e
                );
                return null;
            }
        }

        /**
         * Returns the list of files changed in the new manifest.
         */
        static List<String> diff(@Nullable JarManifest oldManifest, JarManifest newManifest) {
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, Long> it : newManifest.nameToHash().entrySet()) {
                Long oldHash = oldManifest == null ? null : oldManifest.nameToHash().get(it.getKey());
                if (!Objects.equals(it.getValue(), oldHash)) {
                    result.add(it.getKey());
                }
            }
            return result;
        }
    }

    private static final Logger logger =
        Logger.getInstance(JarFileManifest.class);

    /**
     * Returns a per-jar map of files changed in the new manifest
     */
    public static Diff diffJarManifests(@Nullable JarFileManifest oldManifest, JarFileManifest newManifest) {
        MultiMap<Path, String> changedFilesMap = new MultiMap<>();
        for (Map.Entry<Path, JarManifest> entry : newManifest.myJarManifests.entrySet()) {
            // quick test for object equality -- jars are often not rebuilt
            JarManifest old = oldManifest == null ? null : oldManifest.myJarManifests.get(entry.getKey());
            if (Objects.equals(old, entry.getValue())) {
                continue;
            }
            List<String> changedFiles =
                JarManifest.diff(
                    old,
                    entry.getValue()
                );
            if (!changedFiles.isEmpty()) {
                changedFilesMap.put(entry.getKey(), changedFiles);
            }
        }
        return new Diff(/* perJarModifiedFiles = */ changedFilesMap);
    }

    public static JarFileManifest build(Collection<Path> jars, @Nullable JarFileManifest previousManifest) {
        FilesDiff diff = FilesDiff.diffFileTimestamps(previousManifest == null ? null : previousManifest.myJarFileState, jars);
        Map<Path, JarManifest> jarManifests = new LinkedHashMap<>();
        for (Path file : jars) {
            if (!diff.updatedFiles().contains(file)) {
                JarManifest it = previousManifest == null ? null : previousManifest.myJarManifests.get(file);
                if (it != null) {
                    jarManifests.put(file, it);
                }
            }
        }
        for (JarManifest m : buildJarManifests(diff.updatedFiles())) {
            jarManifests.put(m.jar(), m);
        }
        return new JarFileManifest(diff.newFileState(), jarManifests);
    }

    private static List<JarManifest> buildJarManifests(Collection<Path> jars) {
        List<JarManifest> result = new ArrayList<>();
        for (Path jar : jars) {
            JarManifest manifest = JarManifest.build(jar);
            if (manifest != null) {
                result.add(manifest);
            }
        }
        return result;
    }
}
