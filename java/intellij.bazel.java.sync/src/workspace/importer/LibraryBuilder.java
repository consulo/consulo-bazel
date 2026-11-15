package org.jetbrains.bazel.workspace.importer;

import consulo.bazel.workspace.BazelModelBuilder;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource;
import org.jetbrains.bazel.workspacemodel.entities.extensions.BazelLibraryExtensionEntity;
import org.jetbrains.bazel.workspacemodel.entities.extensions.WorkspaceModelTargetKey;
import org.jetbrains.bsp.protocol.WorkspaceLibraries.LibraryItem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

// RC: replaces `LibraryEntityUpdater`; goes straight from `LibraryItem` to `LibraryEntity` + `BazelLibraryExtensionEntity`,
// dropping the old `Library` wrapper
public final class LibraryBuilder {
    // consulo: upstream `typealias LibraryNameProvider = (key: WorkspaceTargetKey) -> String`
    @FunctionalInterface
    public interface LibraryNameProvider extends Function<WorkspaceTargetKey, String> {
    }

    // consulo: upstream `typealias ImportedTargetPredicate = (key: WorkspaceTargetKey) -> Boolean`
    @FunctionalInterface
    public interface ImportedTargetPredicate extends Predicate<WorkspaceTargetKey> {
    }

    private LibraryBuilder() {
    }

    public static List<Library> writeAll(
        List<LibraryItem> libraryItems,
        BazelEntitySource entitySource,
        LibraryNameProvider libraryNameProvider,
        BazelModelBuilder storage,
        ImportedTargetPredicate isTargetImported
    ) {
        List<Library> result = new ArrayList<>(libraryItems.size());
        for (LibraryItem it : libraryItems) {
            result.add(write(it, entitySource, libraryNameProvider, storage, isTargetImported));
        }
        return result;
    }

    public static Library write(
        LibraryItem libraryItem,
        BazelEntitySource entitySource,
        LibraryNameProvider libraryNameProvider,
        BazelModelBuilder storage,
        ImportedTargetPredicate isTargetImported
    ) {
        // consulo: every library is a project library (upstream LibraryTableId.ProjectLibraryTableId)
        String displayName = libraryNameProvider.apply(libraryItem.key());
        // consulo: upstream `storage.resolve(LibraryId)` finds a library added to the diff by this sync,
        // a library of this sync carries its BazelLibraryExtensionEntity in the builder
        Library existing = storage.getLibraryModel().getLibraryByName(displayName);
        if (existing != null && storage.getLibraryData(existing, BazelLibraryExtensionEntity.class) != null) {
            return existing;
        }

        List<String> sourcesRoots = libraryItem.sourceJars().stream()
            .map(LibraryBuilder::toLibraryRoot)
            .toList();
        List<String> classesRoots = fullOrInterfaceJars(libraryItem, isTargetImported).stream()
            .map(LibraryBuilder::toLibraryRoot)
            .toList();

        Library library = storage.getOrCreateLibrary(displayName);
        Library.ModifiableModel libraryModel = storage.getLibraryModifiableModel(library);
        for (String it : sourcesRoots) {
            libraryModel.addRoot(it, SourcesOrderRootType.ID);
        }
        for (String it : classesRoots) {
            libraryModel.addRoot(it, BinariesOrderRootType.ID);
        }
        // consulo: upstream excludedRoots = arrayListOf()
        // TODO consulo: upstream marks the library with ImportedLibraryType (external system imported library kind) and stores
        //  the maven coordinates as its ImportedLibraryProperties (toLibraryPropertiesXml), Consulo has no such library type
        storage.putLibraryData(
            library,
            BazelLibraryExtensionEntity.class,
            new BazelLibraryExtensionEntity(
                WorkspaceModelTargetKey.of(libraryItem.key()),
                libraryItem.key().label().isSynthetic()
            )
        );

        return library;
    }

    /**
     * Prefers the interface jar only for a library that:
     * 1. Comes from a target that is not imported (limited depth case)
     * 2. Comes from a target that this workspace builds
     * 3. Source jars are present - should be true if other conditions hold, but let's ensure it is.
     * <p>
     * Every other library keeps the full jar.
     * <p>
     * The usage of interface jars results in smaller indices, and from the user perspective should not be easily distinguishable from the full jar.
     * <p>
     * Noticeable differences:
     * 1. The lack of resources in the index, which could affect the resources-related inspections.
     * Although, this seems to be an edge case, and in some cases even desired behavior - skipping a high amount of resources that are not needed for any inspections seems to be a nice saving.
     * In case of somebody really needing resources, importing a given target is the solution here e.g., by increasing the import depth.
     * Introducing ijars opt-out option should be the last resort here.
     * 2. The lack of bytecode analysis, which is a natural consequence of using interface jars. It does not seem to be that relevant.
     */
    private static List<Path> fullOrInterfaceJars(LibraryItem receiver, ImportedTargetPredicate isTargetImported) {
        if (receiver.containsInternalJars() && !receiver.sourceJars().isEmpty() && !isTargetImported.test(receiver.key())) {
            return receiver.ijars().isEmpty() ? receiver.jars() : receiver.ijars();
        }
        else {
            return receiver.jars().isEmpty() ? receiver.ijars() : receiver.jars();
        }
    }

    private static String toLibraryRoot(Path receiver) {
        return BuilderUtils.toResolvedVirtualFileUrl(BuilderUtils.toJarUrlString(receiver));
    }
}
