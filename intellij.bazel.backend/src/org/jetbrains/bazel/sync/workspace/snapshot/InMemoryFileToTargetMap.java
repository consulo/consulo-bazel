package org.jetbrains.bazel.sync.workspace.snapshot;

import com.dynatrace.hash4j.hashing.Hashing;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.bazel.commons.BazelInfo;
import org.jetbrains.bazel.commons.RepoMapping;
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver;
import org.jetbrains.bsp.protocol.BuildTarget;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jetbrains.bsp.protocol.OutputLocation.OutputLocationResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// consulo: the upstream FileToTargetMap interface declared in this file lives in FileToTargetMap.java
public class InMemoryFileToTargetMap implements FileToTargetMap {
    private final Long2ObjectMap<ArrayList<WorkspaceTargetKey>> myHash2Targets;

    private final ConcurrentHashMap<Long, List<WorkspaceTargetKey>> myDelta = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();

    InMemoryFileToTargetMap(Long2ObjectMap<ArrayList<WorkspaceTargetKey>> hash2Targets) {
        myHash2Targets = hash2Targets;
    }

    // consulo: internal upstream, read by the persistent snapshot storage
    public ConcurrentHashMap<Long, List<WorkspaceTargetKey>> getDelta() {
        return myDelta;
    }

    public ReentrantReadWriteLock getLock() {
        return myLock;
    }

    @Override
    public List<WorkspaceTargetKey> getTargetsByFile(Path path) {
        myLock.readLock().lock();
        try {
            ArrayList<WorkspaceTargetKey> targets = myHash2Targets.get(hashFilePath(path));
            if (targets != null) {
                return targets;
            }

            // otherwise empty
            return List.of();
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    @Override
    public void addMapping(Path path, List<WorkspaceTargetKey> targets) {
        myLock.writeLock().lock();
        try {
            long hash = hashFilePath(path);
            myHash2Targets.put(hash, new ArrayList<>(targets));
            myDelta.put(hash, new ArrayList<>(targets));
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    @Override
    public void removeMapping(Path path) {
        myLock.writeLock().lock();
        try {
            long hash = hashFilePath(path);
            myHash2Targets.remove(hash);
            myDelta.put(hash, List.of());
        }
        finally {
            myLock.writeLock().unlock();
        }
    }

    @Override
    public int getSize() {
        myLock.readLock().lock();
        try {
            return myHash2Targets.size();
        }
        finally {
            myLock.readLock().unlock();
        }
    }

    // consulo: upstream `operator fun FileToTargetMap.get(path)`
    public static List<WorkspaceTargetKey> get(FileToTargetMap receiver, Path path) {
        return receiver.getTargetsByFile(path);
    }

    public static class File2TargetMapBuilder {
        private final BazelInfo myBazelInfo;
        private final RepoMapping myRepoMapping;

        public File2TargetMapBuilder(BazelInfo bazelInfo, RepoMapping repoMapping) {
            myBazelInfo = bazelInfo;
            myRepoMapping = repoMapping;
        }

        public BazelInfo getBazelInfo() {
            return myBazelInfo;
        }

        public RepoMapping getRepoMapping() {
            return myRepoMapping;
        }

        public FileToTargetMap build(Iterable<BuildTarget> targets) {
            Long2ObjectOpenHashMap<ArrayList<WorkspaceTargetKey>> hash2Targets = new Long2ObjectOpenHashMap<>();
            OutputLocationResolver resolver = DefaultOutputLocationResolver.createLocalWorkspaceOnly(myBazelInfo, myRepoMapping);
            for (BuildTarget target : targets) {
                Iterator<OutputLocation> locations = target.getSources().getOutputLocations().iterator();
                while (locations.hasNext()) {
                    OutputLocation location = locations.next();
                    Path source = resolver.resolve(location, RepoMapping.getLocalRepositories(myRepoMapping));
                    if (source == null) {
                        continue;
                    }
                    hash2Targets.computeIfAbsent(hashFilePath(source), it -> new ArrayList<>())
                        .add(target.getKey());
                }
            }
            return new InMemoryFileToTargetMap(
                hash2Targets
            );
        }

        public static FileToTargetMap build(Map<Path, List<WorkspaceTargetKey>> targets) {
            Long2ObjectOpenHashMap<ArrayList<WorkspaceTargetKey>> hash2Targets = new Long2ObjectOpenHashMap<>();
            for (Map.Entry<Path, List<WorkspaceTargetKey>> entry : targets.entrySet()) {
                hash2Targets.put(hashFilePath(entry.getKey()), new ArrayList<>(entry.getValue()));
            }
            return new InMemoryFileToTargetMap(
                hash2Targets
            );
        }
    }

    private static long hashFilePath(Path path) {
        return Hashing.xxh3_64().hashStream().putString(path.toAbsolutePath().toString()).getAsLong();
    }
}
