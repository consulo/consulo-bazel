package org.jetbrains.bazel.workspace.indexing;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

// consulo: roots are VFS urls (String) instead of workspace model VirtualFileUrl
public interface IndexableContent {
    Set<String> getRecursiveRoots();

    Set<String> getNonRecursiveRoots();

    interface MutableIndexableContent extends IndexableContent {
        @Override
        Set<String> getRecursiveRoots();

        @Override
        Set<String> getNonRecursiveRoots();

        void addAll(IndexableContent content);
    }

    static IndexableContent of(Set<String> recursiveRoots, Set<String> nonRecursiveRoots) {
        return mutable(recursiveRoots, nonRecursiveRoots);
    }

    static IndexableContent of() {
        return of(Set.of(), Set.of());
    }

    static MutableIndexableContent mutable(Set<String> recursiveRoots, Set<String> nonRecursiveRoots) {
        return new IndexableContentImpl(new HashSet<>(recursiveRoots), new HashSet<>(nonRecursiveRoots));
    }

    static MutableIndexableContent mutable() {
        return mutable(Set.of(), Set.of());
    }

    static IndexableContent buildIndexableContent(Consumer<MutableIndexableContent> block) {
        MutableIndexableContent content = mutable();
        block.accept(content);
        return content;
    }

    record IndexableContentImpl(
        Set<String> recursiveRoots,
        Set<String> nonRecursiveRoots
    ) implements MutableIndexableContent {
        @Override
        public Set<String> getRecursiveRoots() {
            return recursiveRoots;
        }

        @Override
        public Set<String> getNonRecursiveRoots() {
            return nonRecursiveRoots;
        }

        @Override
        public void addAll(IndexableContent content) {
            recursiveRoots.addAll(content.getRecursiveRoots());
            nonRecursiveRoots.addAll(content.getNonRecursiveRoots());
        }
    }
}
