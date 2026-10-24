package org.jetbrains.bazel.sync.workspace.snapshot;


import java.nio.file.Path;
import java.util.List;

// consulo: declared in upstream InMemoryFileToTargetMap.kt, own file because InMemoryFileToTargetMap implements it
public interface FileToTargetMap {
    List<WorkspaceTargetKey> getTargetsByFile(Path path);

    // RC: this breaks immutability constrain of entire `WorkspaceSnapshot`
    // however for now it's needed, storage itself is not able to flush changes based on
    // snapshot diff, it's here for pure performance reason, we don't want to flush
    // entire snapshot each time we invoke bazel file listener
    void addMapping(Path path, List<WorkspaceTargetKey> targets);

    void removeMapping(Path path);

    int getSize();

    FileToTargetMap EMPTY = new FileToTargetMap() {
        @Override
        public List<WorkspaceTargetKey> getTargetsByFile(Path path) {
            return List.of();
        }

        @Override
        public void addMapping(Path path, List<WorkspaceTargetKey> targets) {
        }

        @Override
        public void removeMapping(Path path) {
        }

        @Override
        public int getSize() {
            return 0;
        }
    };
}
