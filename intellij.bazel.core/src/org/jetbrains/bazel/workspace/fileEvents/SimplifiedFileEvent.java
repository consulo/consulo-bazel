package org.jetbrains.bazel.workspace.fileEvents;

import consulo.annotation.access.RequiredReadAction;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import org.jetbrains.bazel.commons.LanguageClass;
import org.jetbrains.bazel.commons.constants.Constants;
import org.jetbrains.bazel.ignore.BazelIgnoreService;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract sealed class SimplifiedFileEvent permits SimplifiedFileEvent.Create, SimplifiedFileEvent.CreateDirectory,
    SimplifiedFileEvent.Copy, SimplifiedFileEvent.ContentChange, SimplifiedFileEvent.Delete, SimplifiedFileEvent.Move,
    SimplifiedFileEvent.Rename {
    private final @Nullable Path myFileRemoved;
    private final @Nullable Path myFileAdded;
    private final @Nullable VirtualFile myNewVirtualFile;

    private SimplifiedFileEvent(
        @Nullable Path fileRemoved,
        @Nullable Path fileAdded,
        @Nullable VirtualFile newVirtualFile
    ) {
        myFileRemoved = fileRemoved;
        myFileAdded = fileAdded;
        myNewVirtualFile = newVirtualFile;
    }

    private SimplifiedFileEvent(@Nullable String fileRemoved, @Nullable String fileAdded, @Nullable VirtualFile newVirtualFile) {
        this(
            /* fileRemoved = */ fileRemoved != null ? toNioPathOrNull(fileRemoved) : null,
            /* fileAdded = */ fileAdded != null ? toNioPathOrNull(fileAdded) : null,
            /* newVirtualFile = */ newVirtualFile
        );
    }

    public @Nullable Path getFileRemoved() {
        return myFileRemoved;
    }

    public @Nullable Path getFileAdded() {
        return myFileAdded;
    }

    public @Nullable VirtualFile getNewVirtualFile() {
        return myNewVirtualFile;
    }

    /**
     * The paths that the event affects. A move or a rename affects two.
     */
    public List<Path> affectedPaths() {
        List<Path> result = new ArrayList<>(2);
        if (myFileRemoved != null) {
            result.add(myFileRemoved);
        }
        if (myFileAdded != null) {
            result.add(myFileAdded);
        }
        return result;
    }

    /**
     * Returns true if Bazel reads the file itself, for example a BUILD or a `.bzl` file.
     */
    public boolean affectsBazelConfigFile() {
        return !(this instanceof CreateDirectory) && affectedPaths().stream().anyMatch(SimplifiedFileEvent::isBazelConfigFile);
    }

    /**
     * Returns true if a Bazel target can own the file, so the workspace model may need an update.
     */
    public boolean affectsSourceFile(Project project) {
        // the content of a source file never changes the target that owns it
        return !(this instanceof ContentChange) && affectedPaths().stream().anyMatch(it -> isSourceFile(it, project));
    }

    public boolean shouldBeProcessed(Project project) {
        Path rootPath = org.jetbrains.bazel.config.project.getRootDir(project).toNioPath();
        if (affectedPaths().stream().noneMatch(it -> it.startsWith(rootPath))) {
            return false;
        }

        // Check for ignored files
        if (affectedPaths().stream().allMatch(it -> BazelIgnoreService.getInstance(project).isIgnored(it))) {
            return false;
        }

        // check for files under .bazelbsp
        Path bazelBsp = rootPath.resolve(Constants.DOT_BAZELBSP_DIR_NAME);
        if (affectedPaths().stream().allMatch(it -> it.startsWith(bazelBsp))) {
            return false;
        }

        return !(this instanceof CreateDirectory) || myFileAdded != null && myNewVirtualFile != null && myNewVirtualFile.isValid();
    }

    private boolean isSourceFile(Path receiver, Project project) {
        return LanguageClass.fromExtension(extension(receiver)) != null &&
            !BazelIgnoreService.getInstance(project).isIgnored(receiver);
    }

    public boolean doesAffectFolder(Path folderPath) {
        return myFileRemoved != null && myFileRemoved.startsWith(folderPath) || myFileAdded != null && myFileAdded.startsWith(folderPath);
    }

    @RequiredReadAction
    public boolean affectsExcludedFiles(ProjectFileIndex fileIndex, VirtualFileManager fileManager) {
        if (isExcludedInFileIndex(myNewVirtualFile, fileIndex)) {
            return true;
        }
        Path firstExistingAncestor = myFileRemoved != null ? getFirstExistingAncestor(myFileRemoved) : null;
        VirtualFile ancestorFile = firstExistingAncestor != null ? fileManager.findFileByNioPath(firstExistingAncestor) : null;
        return isExcludedInFileIndex(ancestorFile, fileIndex);
    }

    private static boolean isExcludedInFileIndex(@Nullable VirtualFile receiver, ProjectFileIndex fileIndex) {
        return receiver != null && fileIndex.isExcluded(receiver);
    }

    private static @Nullable Path getFirstExistingAncestor(Path receiver) {
        Path ancestor = receiver.getParent();
        while (ancestor != null && !Files.exists(ancestor)) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimplifiedFileEvent otherEvent)) {
            return false;
        }

        if (!Objects.equals(myFileRemoved, otherEvent.myFileRemoved)) {
            return false;
        }
        if (!Objects.equals(myFileAdded, otherEvent.myFileAdded)) {
            return false;
        }
        if (!Objects.equals(myNewVirtualFile, otherEvent.myNewVirtualFile)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(myFileRemoved);
        result = 31 * result + Objects.hashCode(myFileAdded);
        result = 31 * result + Objects.hashCode(myNewVirtualFile);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(removed=" + myFileRemoved + ", added=" + myFileAdded + ")";
    }

    public static final class Create extends SimplifiedFileEvent {
        public Create(String path, @Nullable VirtualFile vFile) {
            super((String) null, /* fileAdded = */ path, /* newVirtualFile = */ vFile);
        }

        public Create(VFileCreateEvent originalEvent) {
            this(originalEvent.getPath(), originalEvent.getFile());
        }
    }

    public static final class CreateDirectory extends SimplifiedFileEvent {
        public CreateDirectory(VFileCreateEvent originalEvent) {
            super((String) null, /* fileAdded = */ originalEvent.getPath(), /* newVirtualFile = */ originalEvent.getFile());
        }
    }

    public static final class Copy extends SimplifiedFileEvent {
        public Copy(VFileCopyEvent originalEvent) {
            super((String) null, /* fileAdded = */ originalEvent.getPath(), /* newVirtualFile = */ originalEvent.findCreatedFile());
        }
    }

    /**
     * A change of the file content, in practice a save.
     * <p>
     * Bazel reads the new content of a configuration file, so the plugin must see the save. The path
     * goes into {@code fileAdded}, because the file is present after the event. {@link #affectsSourceFile} is
     * always false, so such an event never reaches the workspace model update.
     */
    public static final class ContentChange extends SimplifiedFileEvent {
        public ContentChange(VFileContentChangeEvent originalEvent) {
            super((String) null, /* fileAdded = */ originalEvent.getPath(), /* newVirtualFile = */ originalEvent.getFile());
        }
    }

    public static final class Delete extends SimplifiedFileEvent {
        public Delete(VFileDeleteEvent originalEvent) {
            super(/* fileRemoved = */ originalEvent.getPath(), (String) null, null);
        }
    }

    public static final class Move extends SimplifiedFileEvent {
        public Move(VFileMoveEvent originalEvent) {
            super(
                /* fileRemoved = */ originalEvent.getOldPath(),
                /* fileAdded = */ originalEvent.getNewPath(),
                /* newVirtualFile = */ originalEvent.getFile()
            );
        }
    }

    public static final class Rename extends SimplifiedFileEvent {
        public Rename(VFilePropertyChangeEvent originalEvent) {
            super(
                /* fileRemoved = */ originalEvent.getOldPath(),
                /* fileAdded = */ originalEvent.getNewPath(),
                /* newVirtualFile = */ originalEvent.getFile()
            );
        }
    }

    public static @Nullable SimplifiedFileEvent from(VFileEvent event) {
        if (event instanceof VFileCreateEvent createEvent) {
            if (createEvent.isDirectory()) {
                return new CreateDirectory(createEvent);
            }
            else {
                return new Create(createEvent);
            }
        }
        else if (event instanceof VFileCopyEvent copyEvent) {
            return new Copy(copyEvent);
        }
        else if (event instanceof VFileContentChangeEvent contentChangeEvent) {
            return new ContentChange(contentChangeEvent);
        }
        else if (event instanceof VFileDeleteEvent deleteEvent) {
            return new Delete(deleteEvent);
        }
        else if (event instanceof VFileMoveEvent moveEvent) {
            return new Move(moveEvent);
        }
        else if (event instanceof VFilePropertyChangeEvent propertyChangeEvent) {
            if (VirtualFile.PROP_NAME.equals(propertyChangeEvent.getPropertyName())) { // file rename
                return new Rename(propertyChangeEvent);
            }
            else { // property change other than file rename
                return null;
            }
        }
        else {
            return null;
        }
    }

    public static final Set<String> SUPPORTED_CONFIG_FILE_NAMES = Set.copyOf(Arrays.asList(Constants.SUPPORTED_CONFIG_FILE_NAMES));

    /**
     * Returns true if {@code file} names a Bazel configuration file.
     */
    public static boolean isBazelConfigFile(Path file) {
        Path fileName = file.getFileName();
        return fileName != null && SUPPORTED_CONFIG_FILE_NAMES.contains(fileName.toString()) ||
            Arrays.asList(Constants.SUPPORTED_EXTENSIONS).contains(extension(file));
    }

    // consulo: IntelliJ String.toNioPathOrNull()
    private static @Nullable Path toNioPathOrNull(String path) {
        try {
            return Path.of(path);
        }
        catch (InvalidPathException e) {
            return null;
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
        return index < 0 ? "" : name.substring(index + 1);
    }
}
