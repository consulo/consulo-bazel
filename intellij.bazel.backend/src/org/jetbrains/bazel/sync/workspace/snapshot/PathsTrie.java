package org.jetbrains.bazel.sync.workspace.snapshot;

import consulo.util.interner.Interner;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Traditional path trie, the paths share their common prefix through {@link TrieNode} children.
 */
public class PathsTrie {
    public static final String SEPARATOR = "/";

    private final TrieNode myRoot;

    public PathsTrie() {
        this(new TrieNode(""));
    }

    public PathsTrie(TrieNode root) {
        myRoot = root;
    }

    public TrieNode getRoot() {
        return myRoot;
    }

    public boolean isEmpty() {
        return myRoot.isEmpty();
    }

    public void insert(Iterable<String> segments) {
        TrieNode node = myRoot;
        for (String segment : segments) {
            node = node.findOrInsertChild(segment);
        }
        node.setTerminal(true);
    }

    public void insertPath(String path) {
        // consulo: Kotlin String.split(delimiter) keeps empty entries and does not use regular expressions
        insert(Arrays.asList(path.split(Pattern.quote(SEPARATOR), -1)));
    }

    public Stream<String> paths() {
        // consulo: Kotlin sequence { } -> lazily evaluated iterator
        return StreamSupport.stream(
            () -> Spliterators.spliteratorUnknownSize(new PathsIterator(myRoot), Spliterator.ORDERED | Spliterator.NONNULL),
            Spliterator.ORDERED | Spliterator.NONNULL,
            false
        );
    }

    private static final class PathsIterator implements Iterator<String> {
        // `null` marks trie root
        private final Deque<Pair<@Nullable String, TrieNode>> myStack = new ArrayDeque<>();
        private @Nullable String myNext;

        private PathsIterator(TrieNode root) {
            myStack.addLast(Pair.<@Nullable String, TrieNode>create(null, root));
        }

        @Override
        public boolean hasNext() {
            while (myNext == null && !myStack.isEmpty()) {
                Pair<@Nullable String, TrieNode> entry = myStack.removeLast();
                String path = entry.getFirst();
                TrieNode node = entry.getSecond();
                if (node.isTerminal()) {
                    myNext = path != null ? path : "";
                }
                List<TrieNode> children = node.getChildren();
                if (children == null) {
                    continue;
                }
                for (TrieNode child : children) {
                    String childPath = path == null ? child.getSegment() : path + SEPARATOR + child.getSegment();
                    myStack.addLast(Pair.create(childPath, child));
                }
            }
            return myNext != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String result = Objects.requireNonNull(myNext);
            myNext = null;
            return result;
        }
    }

    public static class TrieNode {
        private static final Interner<String> INTERNER = Interner.createWeakInterner();

        private final String mySegment;
        private @Nullable List<TrieNode> myChildren;
        private boolean myIsTerminal;

        public TrieNode(String segment) {
            this(segment, null, false);
        }

        public TrieNode(String segment, @Nullable List<TrieNode> children, boolean isTerminal) {
            mySegment = segment;
            myChildren = children;
            myIsTerminal = isTerminal;
        }

        public String getSegment() {
            return mySegment;
        }

        public @Nullable List<TrieNode> getChildren() {
            return myChildren;
        }

        public void setChildren(@Nullable List<TrieNode> children) {
            myChildren = children;
        }

        public boolean isTerminal() {
            return myIsTerminal;
        }

        public void setTerminal(boolean terminal) {
            myIsTerminal = terminal;
        }

        public boolean isEmpty() {
            List<TrieNode> children = myChildren;
            return !myIsTerminal && (children == null || children.isEmpty());
        }

        public @Nullable TrieNode findChild(String segment) {
            List<TrieNode> children = myChildren;
            if (children == null) {
                return null;
            }
            int idx = binarySearch(children, segment);
            return idx >= 0 ? children.get(idx) : null;
        }

        public TrieNode findOrInsertChild(String segment) {
            List<TrieNode> children = getOrCreateChildren();
            int idx = binarySearch(children, segment);
            if (idx >= 0) {
                return children.get(idx);
            }
            TrieNode node = new TrieNode(INTERNER.intern(segment));
            children.add(-(idx + 1), node);
            return node;
        }

        public List<TrieNode> getOrCreateChildren() {
            List<TrieNode> children = myChildren;
            if (children == null) {
                children = new ArrayList<>();
                myChildren = children;
            }
            return children;
        }

        // consulo: Kotlin List.binarySearch { it.segment.compareTo(segment) }
        private static int binarySearch(List<TrieNode> children, String segment) {
            int low = 0;
            int high = children.size() - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int cmp = children.get(mid).getSegment().compareTo(segment);
                if (cmp < 0) {
                    low = mid + 1;
                }
                else if (cmp > 0) {
                    high = mid - 1;
                }
                else {
                    return mid;
                }
            }
            return -(low + 1);
        }
    }
}
