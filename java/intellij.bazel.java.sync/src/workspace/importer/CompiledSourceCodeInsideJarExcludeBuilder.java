package org.jetbrains.bazel.workspace.importer;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.library.Library;
import consulo.virtualFileSystem.StandardFileSystems;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity.CompiledSourceCodeInsideJarExcludeId;
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity.LibraryCompiledSourceCodeInsideJarExcludeEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelLibraryExtensionEntity;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

// https://youtrack.jetbrains.com/issue/BAZEL-1672
// RC: replaces `CompiledSourceCodeInsideJarExcludeTransformer` + `CompiledSourceCodeInsideJarExcludeEntityUpdater`
public final class CompiledSourceCodeInsideJarExcludeBuilder {
    private CompiledSourceCodeInsideJarExcludeBuilder() {
    }

    public static void write(
        Collection<BuildTarget> targets,
        List<LibraryItem> libraries,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        BazelModelBuilder storage
    ) {
        write(targets, libraries, resolveLocation, storage, null);
    }

    /**
     * writes a {@link CompiledSourceCodeInsideJarExcludeEntity} plus a
     * {@link LibraryCompiledSourceCodeInsideJarExcludeEntity} per existing {@code LibraryEntity} in {@code storage}.
     * <p>
     * returns without writing anything when there are no libraries from internal targets that need
     * source-code exclusion.
     * <p>
     * must run after {@link LibraryBuilder} so the per-library link entities have something to reference.
     * <p>
     * {@code currentExcludeEntity} is the previous exclude entity (if any). when the content is unchanged,
     * its ID is reused; otherwise the ID is a hash of the content, so referring entities invalidate and
     * `CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor` re-runs.
     * the contributor caches data by the ID, so the ID must be different for different content
     * also among the open projects and the IDs loaded from the workspace model cache.
     * <p>
     * consulo: the entities are the project data / library data of {@link BazelModelBuilder}
     */
    public static void write(
        Collection<BuildTarget> targets,
        List<LibraryItem> libraries,
        Function<OutputLocation, @Nullable Path> resolveLocation,
        BazelModelBuilder storage,
        @Nullable CompiledSourceCodeInsideJarExcludeEntity currentExcludeEntity
    ) {
        Set<String> librariesFromInternalTargetsUrls = calculateLibrariesFromInternalTargetsUrls(libraries);
        if (librariesFromInternalTargetsUrls.isEmpty()) {
            return;
        }

        Set<String> relativePathsInsideJarToExclude = calculateSourceFilePaths(targets, resolveLocation);

        CompiledSourceCodeInsideJarExcludeId nextExcludeEntityId;
        if (currentExcludeEntity != null &&
            currentExcludeEntity.relativePathsInsideJarToExclude().equals(relativePathsInsideJarToExclude) &&
            currentExcludeEntity.librariesFromInternalTargetsUrls().equals(librariesFromInternalTargetsUrls)
        ) {
            nextExcludeEntityId = currentExcludeEntity.excludeId();
        }
        else {
            // Change the ID so that all referring entities (LibraryCompiledSourceCodeInsideJarExcludeEntity)
            // are invalidated and CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor re-runs on them.
            nextExcludeEntityId = contentExcludeId(relativePathsInsideJarToExclude, librariesFromInternalTargetsUrls);
        }

        CompiledSourceCodeInsideJarExcludeEntity excludeEntity = new CompiledSourceCodeInsideJarExcludeEntity(
            relativePathsInsideJarToExclude,
            librariesFromInternalTargetsUrls,
            nextExcludeEntityId
        );
        storage.putProjectData(CompiledSourceCodeInsideJarExcludeEntity.class, excludeEntity);

        // consulo: upstream iterates every LibraryEntity of the diff storage, i.e. the libraries created by this sync;
        // they are the project libraries which carry a BazelLibraryExtensionEntity in the builder
        for (Library library : storage.getLibraryModel().getLibraries()) {
            String libraryName = library.getName();
            if (libraryName == null || storage.getLibraryData(library, BazelLibraryExtensionEntity.class) == null) {
                continue;
            }
            storage.putLibraryData(
                library,
                LibraryCompiledSourceCodeInsideJarExcludeEntity.class,
                new LibraryCompiledSourceCodeInsideJarExcludeEntity(
                    libraryName,
                    excludeEntity.getSymbolicId()
                )
            );
        }
    }

    /**
     * The same content gives the same ID, so the ID does not depend on the order of the syncs or on the open projects.
     */
    private static CompiledSourceCodeInsideJarExcludeId contentExcludeId(Set<String> sourceFilePaths, Set<String> libraryUrls) {
        // consulo: the platform Guava (30.1) has no murmur3_32_fixed; murmur3_32 differs only for supplementary characters,
        //   the ID only has to be stable within one installation
        @SuppressWarnings("deprecation")
        Hasher hasher = Hashing.murmur3_32().newHasher();
        for (Set<String> values : List.of(sourceFilePaths, libraryUrls)) {
            for (String value : values.stream().sorted().toList()) {
                hasher.putString(value, StandardCharsets.UTF_8);
                hasher.putByte((byte) 0);
            }
            hasher.putByte((byte) 1);
        }
        return new CompiledSourceCodeInsideJarExcludeId(hasher.hash().asInt());
    }

    /**
     * The package of a source file is unknown, so the paths of the source files are stored instead of the paths inside jars.
     * {@link CompiledSourceFileIndex} approximates the package by the relative path inside the jar and matches it against them.
     */
    public static Set<String> calculateSourceFilePaths(
        Collection<BuildTarget> targets,
        Function<OutputLocation, @Nullable Path> resolveLocation
    ) {
        HashSet<String> result = new HashSet<>();
        for (BuildTarget target : targets) {
            List<Path> sources = BuildTarget.nonGeneratedSources(target).map(resolveLocation).filter(Objects::nonNull).toList();
            for (Path source : sources) {
                String extension = BuilderUtils.extension(source);
                if (extension.equals("java") || extension.equals("kt")) {
                    result.add(invariantSeparatorsPathString(source));
                }
            }
        }
        return result;
    }

    public static Set<String> calculateLibrariesFromInternalTargetsUrls(List<LibraryItem> libraryItems) {
        Set<String> result = new LinkedHashSet<>();
        for (LibraryItem it : libraryItems) {
            if (!it.containsInternalJars()) {
                continue;
            }
            List<Path> jars = new ArrayList<>(it.jars());
            jars.addAll(it.ijars());
            jars.addAll(it.sourceJars());
            for (Path jarPath : jars) {
                result.add(StandardFileSystems.JAR_PROTOCOL_PREFIX + invariantSeparatorsPathString(jarPath) + StandardFileSystems.JAR_SEPARATOR);
            }
        }
        return result;
    }

    // consulo: Kotlin Path.invariantSeparatorsPathString
    private static String invariantSeparatorsPathString(Path path) {
        String separator = path.getFileSystem().getSeparator();
        return separator.equals("/") ? path.toString() : path.toString().replace(separator, "/");
    }

    /**
     * Tells whether a file inside a jar is compiled from (or is a copy of) a source file of the project.
     * <p>
     * The package of the file inside the jar is approximated by its relative path inside the jar:
     * `com/example/Foo.class` corresponds to any source file `.../com/example/Foo.java`.
     */
    public static class CompiledSourceFileIndex {
        // source file name -> directories which contain a source file with this name
        private final Map<String, List<String>> myDirectoriesByFileName;

        public CompiledSourceFileIndex(Collection<String> sourceFilePaths) {
            Map<String, List<String>> directoriesByFileName = new LinkedHashMap<>();
            for (String it : sourceFilePaths) {
                int slash = it.lastIndexOf('/');
                String fileName = it.substring(slash + 1);
                String directory = slash == -1 ? "" : it.substring(0, slash);
                directoriesByFileName.computeIfAbsent(fileName, ignored -> new ArrayList<>()).add(directory);
            }
            myDirectoriesByFileName = directoriesByFileName;
        }

        /**
         * {@code relativePathInsideJar} must not point at a nested class, e.g., `com/example/Foo$Bar.class` should be passed as
         * `com/example/Foo.class`.
         */
        public boolean hasSourceFor(String relativePathInsideJar) {
            int slash = relativePathInsideJar.lastIndexOf('/');
            String packageDirectory = slash == -1 ? "" : relativePathInsideJar.substring(0, slash);
            String fileName = relativePathInsideJar.substring(slash + 1);
            for (String sourceFileName : sourceFileNameCandidates(fileName)) {
                List<String> directories = myDirectoriesByFileName.get(sourceFileName);
                if (directories != null && directories.stream().anyMatch(it -> endsWithPackageDirectory(it, packageDirectory))) {
                    return true;
                }
            }
            return false;
        }

        private static List<String> sourceFileNameCandidates(String fileName) {
            int dot = fileName.lastIndexOf('.');
            String nameWithoutExtension = dot == -1 ? fileName : fileName.substring(0, dot);
            String extension = dot == -1 ? "" : fileName.substring(dot + 1);
            switch (extension) {
                case "java", "kt" -> {
                    return List.of(fileName);
                }
                case "class" -> {
                    List<String> result = new ArrayList<>();
                    result.add(nameWithoutExtension + ".java");
                    result.add(nameWithoutExtension + ".kt");
                    // E.g., MainKt.class -> main.kt or Main.kt
                    String kotlinFileName = nameWithoutExtension.endsWith("Kt")
                        ? nameWithoutExtension.substring(0, nameWithoutExtension.length() - 2)
                        : nameWithoutExtension;
                    if (!kotlinFileName.equals(nameWithoutExtension) && !kotlinFileName.isEmpty()) {
                        result.add(kotlinFileName + ".kt");
                        result.add(kotlinFileName.substring(0, 1).toLowerCase(Locale.US) + kotlinFileName.substring(1) + ".kt");
                    }
                    return result;
                }
                default -> {
                    return List.of();
                }
            }
        }

        private static boolean endsWithPackageDirectory(String receiver, String packageDirectory) {
            return packageDirectory.isEmpty() || receiver.equals(packageDirectory) || receiver.endsWith("/" + packageDirectory);
        }
    }
}
