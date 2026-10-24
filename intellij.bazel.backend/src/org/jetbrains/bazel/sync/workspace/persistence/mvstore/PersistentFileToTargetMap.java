package org.jetbrains.bazel.sync.workspace.persistence.mvstore;

import com.dynatrace.hash4j.hashing.Hashing;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.SnapshotStorage.SnapshotGeneration;
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap;
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// consulo: internal upstream, public for the top-level hashFilePath()
public final class PersistentFileToTargetMap implements FileToTargetMap {
    private final PersistentWorkspaceSnapshot myPartialSnapshot;
    private final SnapshotGeneration myGeneration;

    /**
     * Live changes to {@link FileToTargetMap} performed using {@link FileToTargetMap#addMapping} and {@link FileToTargetMap#removeMapping},
     * are accumulated into {@code delta} map in order to prevent full rebuilds on entire {@link FileToTargetMap} on each small change.
     * This was mainly done to prevent full snapshot saves even on smallest changes performed by bazel file listener.
     */
    private final ConcurrentHashMap<Long, List<WorkspaceTargetKey>> myDelta;

    PersistentFileToTargetMap(PersistentWorkspaceSnapshot partialSnapshot, SnapshotGeneration generation) {
        this(partialSnapshot, generation, new ConcurrentHashMap<>());
    }

    PersistentFileToTargetMap(
        PersistentWorkspaceSnapshot partialSnapshot,
        SnapshotGeneration generation,
        ConcurrentHashMap<Long, List<WorkspaceTargetKey>> delta
    ) {
        myPartialSnapshot = partialSnapshot;
        myGeneration = generation;
        myDelta = delta;
    }

    PersistentWorkspaceSnapshot getPartialSnapshot() {
        return myPartialSnapshot;
    }

    SnapshotGeneration getGeneration() {
        return myGeneration;
    }

    ConcurrentHashMap<Long, List<WorkspaceTargetKey>> getDelta() {
        return myDelta;
    }

    @Override
    public List<WorkspaceTargetKey> getTargetsByFile(Path path) {
        List<WorkspaceTargetKey> pending = myDelta.get(hashFilePath(path));
        if (pending != null) {
            return pending;
        }
        return myGeneration.findTargetsByFile(myPartialSnapshot, path).toList();
    }

    @Override
    public void addMapping(Path path, List<WorkspaceTargetKey> targets) {
        myDelta.put(hashFilePath(path), targets);
    }

    @Override
    public void removeMapping(Path path) {
        myDelta.put(hashFilePath(path), List.of());
    }

    @Override
    public int getSize() {
        return myGeneration.fileCount();  // stats-only, small pending deltas are not counted
    }

    public void flush() {
        for (Map.Entry<Long, List<WorkspaceTargetKey>> entry : myDelta.entrySet()) {
            long hash = entry.getKey();
            List<WorkspaceTargetKey> keys = entry.getValue();
            if (keys.isEmpty()) {
                myGeneration.removeFileMapping(hash);
            }
            else {
                IntArrayList keyIds = new IntArrayList(keys.size());
                for (WorkspaceTargetKey key : keys) {
                    int keyId = myPartialSnapshot.getKeyId2Target().getReverseOrDefault(key, -1);
                    if (keyId > 0) {
                        keyIds.add(keyId);
                    }
                }
                if (keyIds.isEmpty()) {
                    myGeneration.removeFileMapping(hash);
                }
                else {
                    myGeneration.putFileMapping(hash, keyIds);
                }
            }
            myDelta.remove(hash, keys);
        }
    }

    public static long hashFilePath(Path path) {
        return Hashing.xxh3_64().hashStream().putString(path.toAbsolutePath().toString()).getAsLong();
    }
}
