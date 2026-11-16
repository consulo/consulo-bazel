/*
 * Copyright 2026 JetBrains s.r.o.
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
package com.intellij.aspect.lib;

import org.jspecify.annotations.Nullable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * consulo: top-level functions of upstream sdk/lib/deploy.kt. {@code AspectConfig} is {@link AspectConfig}.
 */
public final class Deploy {
    /**
     * Location of the bundled aspect archive inside the jar.
     */
    private static final String BUNDLED_ASPECT = "/archive_ide.zip";

    /**
     * Canonical name of the platforms module repository, Bazel does not add a version suffix.
     */
    private static final String PLATFORMS_CANONICAL_NAME = "@@platforms";

    private Deploy() {
    }

    /**
     * Deploy the bundled aspect archive (the zip inside the jar) to a workspace directory.
     *
     * @throws IOException if extraction or file operations fail
     * @see #deployAspectZip(Path, Path, AspectConfig, Path)
     */
    public static void deployAspectZip(Path workspaceRoot, Path relativeDestination, AspectConfig config) throws IOException {
        deployAspectZip(workspaceRoot, relativeDestination, config, null);
    }

    /**
     * Deploy an aspect archive to a workspace directory.
     * <p>
     * Extracts all files from the archive, rewrites their load statements using the
     * provided transformers, and generates the aspect configuration. If the archiveZip
     * is null, the bundled zip inside the jar will be deployed.
     *
     * @throws IOException if extraction or file operations fail
     */
    public static void deployAspectZip(
        Path workspaceRoot,
        Path relativeDestination,
        AspectConfig config,
        @Nullable Path archiveZip
    ) throws IOException {
        require(!relativeDestination.isAbsolute());
        require(archiveZip == null || extension(archiveZip).equals("zip"));

        Path destination = workspaceRoot.resolve(relativeDestination);
        Files.createDirectories(destination);

        List<Transformer> transformers = new ArrayList<>(List.of(
            new TransformRelativePaths(relativeDestination),
            new TransformExternalRepositories(config.repoMapping())
        ));

        if (config.useBuiltin().contains(Rules.CC)) {
            transformers.add(TransformCcToolchainType.INSTANCE);
        }
        if (config.useBuiltin().contains(Rules.PYTHON)) {
            transformers.add(TransformPythonToolchainType.INSTANCE);
        }
        if (config.useBuiltin().contains(Rules.JAVA)) {
            transformers.add(TransformJavaSemantics.INSTANCE);
        }
        if (config.repoMapping().containsKey(Rules.SCALA)) {
            transformers.add(new TransformScalaToolchainType(Objects.requireNonNull(config.repoMapping().get(Rules.SCALA))));
        }

        if (config.repoMapping().containsKey(Rules.PLATFORMS)) {
            transformers.add(new TransformPlatformsRepository(Objects.requireNonNull(config.repoMapping().get(Rules.PLATFORMS))));
        }
        else if (config.useBuiltin().contains(Rules.PLATFORMS)) {
            // the platforms module is always part of the dependency graph and has a fixed canonical name
            transformers.add(new TransformPlatformsRepository(PLATFORMS_CANONICAL_NAME));
        }

        transformers.add(new TransformBuiltinRules(config.useBuiltin()));

        InputStream archiveStream;
        if (archiveZip == null) {
            archiveStream = config.getClass().getResourceAsStream(BUNDLED_ASPECT);
        }
        else {
            archiveStream = Files.newInputStream(archiveZip);
        }
        if (archiveStream == null) {
            throw new IllegalArgumentException("Required value was null.");
        }

        extractZipArchive(destination, archiveStream, transformers);
        Config.writeAspectConfig(destination, relativeDestination, config);
    }

    private static void extractZipArchive(
        Path destination,
        InputStream archiveZip,
        List<Transformer> transformers
    ) throws IOException {
        Files.createDirectories(destination);

        try (ZipInputStream stream = new ZipInputStream(archiveZip)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                }
                else {
                    String payload = Loads.transformFile(nonClosing(stream), transformers);
                    Utils.writeFileIfContentDiffers(target, payload);
                }
            }
        }
    }

    /**
     * Returns a view of this {@link InputStream} whose {@link InputStream#close()} is a no-op.
     * <p>
     * Useful when handing the stream to a consumer that closes it, while the caller
     * needs to keep the underlying stream open (e.g. iterating a {@link ZipInputStream}).
     */
    private static InputStream nonClosing(InputStream stream) {
        return new FilterInputStream(stream) {
            @Override
            public void close() {
                /* no-op */
            }
        };
    }

    // consulo: Kotlin require()
    private static void require(boolean value) {
        if (!value) {
            throw new IllegalArgumentException("Failed requirement.");
        }
    }

    // consulo: Kotlin Path.extension
    private static String extension(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        String name = fileName.toString();
        int index = name.lastIndexOf('.');
        return index == -1 ? "" : name.substring(index + 1);
    }
}
