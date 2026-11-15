package org.jetbrains.bazel.languages.starlark.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class GraphUtils {
    private GraphUtils() {
    }

    // Tarjan's algorithm for computing cyclic strongly connected components.
    public static <T> List<List<T>> findCyclicStronglyConnectedComponents(
        Collection<T> nodes,
        Function<T, ? extends Collection<T>> edges
    ) {
        TarjanState<T> state = new TarjanState<>(edges);

        for (T it : nodes) {
            if (!state.myIndexByNode.containsKey(it)) {
                state.visit(it);
            }
        }

        return state.myComponents;
    }

    // consulo: holder for the local variables and the local function visit() of findCyclicStronglyConnectedComponents()
    private static final class TarjanState<T> {
        private final Function<T, ? extends Collection<T>> myEdges;
        private int myNextIndex = 0;
        private final Map<T, Integer> myIndexByNode = new HashMap<>();
        private final Map<T, Integer> myLowLinkByNode = new HashMap<>();
        private final Deque<T> myStack = new ArrayDeque<>();
        private final Set<T> myOnStack = new HashSet<>();
        private final List<List<T>> myComponents = new ArrayList<>();

        private TarjanState(Function<T, ? extends Collection<T>> edges) {
            myEdges = edges;
        }

        private void visit(T node) {
            myIndexByNode.put(node, myNextIndex);
            myLowLinkByNode.put(node, myNextIndex);
            myNextIndex++;

            myStack.addLast(node);
            myOnStack.add(node);

            for (T target : myEdges.apply(node)) {
                if (!myIndexByNode.containsKey(target)) {
                    visit(target);
                    myLowLinkByNode.put(node, Math.min(getValue(myLowLinkByNode, node), getValue(myLowLinkByNode, target)));
                }
                else if (myOnStack.contains(target)) {
                    myLowLinkByNode.put(node, Math.min(getValue(myLowLinkByNode, node), getValue(myIndexByNode, target)));
                }
            }

            if (getValue(myLowLinkByNode, node) == getValue(myIndexByNode, node)) {
                List<T> component = new ArrayList<>();
                T target;
                do {
                    target = myStack.removeLast();
                    myOnStack.remove(target);
                    component.add(target);
                }
                while (!Objects.equals(target, node));

                boolean hasSelfLoop = component.size() == 1 && myEdges.apply(component.get(0)).contains(component.get(0));
                if (component.size() > 1 || hasSelfLoop) {
                    myComponents.add(component);
                }
            }
        }

        // consulo: Kotlin Map.getValue()
        private static <K> int getValue(Map<K, Integer> map, K key) {
            Integer value = map.get(key);
            if (value == null) {
                throw new NoSuchElementException("Key " + key + " is missing in the map.");
            }
            return value;
        }
    }
}
