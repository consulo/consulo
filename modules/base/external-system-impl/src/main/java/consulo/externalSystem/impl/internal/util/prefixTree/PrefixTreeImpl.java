// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import consulo.externalSystem.impl.internal.util.prefixTree.map.AbstractPrefixTreeMap;
import consulo.util.collection.FList;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PrefixTreeImpl<Key, Value extends @Nullable Object> extends AbstractPrefixTreeMap<List<Key>, Value>
    implements MutablePrefixTree<Key, Value> {

    private int mySize = 0;

    private NodeState<Value> myState = NodeState.empty();

    private final Map<Key, PrefixTreeImpl<Key, Value>> myChildren = new LinkedHashMap<>();

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public Set<Map.Entry<List<Key>, Value>> entrySet() {
        return getTreeEntries();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        return key instanceof List<?> list && getValue((List<Key>) list).isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Value get(Object key) {
        if (!(key instanceof List<?> list)) {
            return null;
        }
        return getValue((List<Key>) list).getOrNull();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Value getOrDefault(Object key, Value defaultValue) {
        if (!(key instanceof List<?> list)) {
            return defaultValue;
        }
        return getValue((List<Key>) list).getOrDefault(defaultValue);
    }

    @Override
    public @Nullable Value put(List<Key> key, Value value) {
        return setValue(0, key, value).getOrNull();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Value remove(Object key) {
        if (!(key instanceof List<?> list)) {
            return null;
        }
        return removeValue(0, (List<Key>) list).getOrNull();
    }

    @Override
    public Set<Map.Entry<List<Key>, Value>> getAncestorEntries(List<Key> key) {
        return getAncestorTreeEntries(key);
    }

    @Override
    public Set<Map.Entry<List<Key>, Value>> getDescendantEntries(List<Key> key) {
        return getDescendantTreeEntries(key);
    }

    @Override
    public Set<Map.Entry<List<Key>, Value>> getRootEntries() {
        return getRootTreeEntries();
    }

    private NodeState<Value> getValue(List<Key> key) {
        PrefixTreeImpl<Key, Value> node = findNode(key);
        return node != null ? node.myState : NodeState.empty();
    }

    private NodeState<Value> setValue(int index, List<Key> key, Value value) {
        if (index < 0 || index > key.size()) {
            throw new IllegalArgumentException("Index " + index + " out of bound [0, " + key.size() + "]");
        }
        if (index == key.size()) {
            NodeState<Value> previousState = myState;
            myState = NodeState.of(value);
            if (!previousState.isPresent()) {
                mySize += 1;
            }
            return previousState;
        }
        PrefixTreeImpl<Key, Value> childNode = myChildren.computeIfAbsent(key.get(index), it -> new PrefixTreeImpl<>());
        NodeState<Value> previousState = childNode.setValue(index + 1, key, value);
        if (!previousState.isPresent()) {
            mySize += 1;
        }
        return previousState;
    }

    private NodeState<Value> removeValue(int index, List<Key> key) {
        if (index < 0 || index > key.size()) {
            throw new IllegalArgumentException("Index " + index + " out of bound [0, " + key.size() + "]");
        }
        if (index == key.size()) {
            NodeState<Value> previousState = myState;
            myState = NodeState.empty();
            if (previousState.isPresent()) {
                mySize -= 1;
            }
            return previousState;
        }
        PrefixTreeImpl<Key, Value> childNode = myChildren.get(key.get(index));
        if (childNode == null) {
            return NodeState.empty();
        }
        NodeState<Value> previousState = childNode.removeValue(index + 1, key);
        if (childNode.isEmpty()) {
            myChildren.remove(key.get(index));
        }
        if (previousState.isPresent()) {
            mySize -= 1;
        }
        return previousState;
    }

    private Set<Map.Entry<List<Key>, Value>> getTreeEntries() {
        Set<Map.Entry<List<Key>, Value>> result = new LinkedHashSet<>();
        traverseTree(node -> {
            if (node.state().isPresent()) {
                result.add(new TreeEntry<>(node.key(), node.state().get()));
            }
            return TraverseDecision.CONTINUE;
        });
        return result;
    }

    private Set<Map.Entry<List<Key>, Value>> getAncestorTreeEntries(List<Key> key) {
        Set<Map.Entry<List<Key>, Value>> result = new LinkedHashSet<>();
        traverseNode(key, node -> {
            if (node.state().isPresent()) {
                result.add(new TreeEntry<>(node.key(), node.state().get()));
            }
        });
        return result;
    }

    private Set<Map.Entry<List<Key>, Value>> getDescendantTreeEntries(List<Key> key) {
        PrefixTreeImpl<Key, Value> node = findNode(key);
        if (node == null) {
            return Collections.emptySet();
        }
        Set<Map.Entry<List<Key>, Value>> result = new LinkedHashSet<>();
        for (Map.Entry<List<Key>, Value> entry : node.getTreeEntries()) {
            List<Key> entryKey = new ArrayList<>(key.size() + entry.getKey().size());
            entryKey.addAll(key);
            entryKey.addAll(entry.getKey());
            result.add(new TreeEntry<>(entryKey, entry.getValue()));
        }
        return result;
    }

    private Set<Map.Entry<List<Key>, Value>> getRootTreeEntries() {
        Set<Map.Entry<List<Key>, Value>> result = new LinkedHashSet<>();
        traverseTree(node -> {
            if (node.state().isPresent()) {
                result.add(new TreeEntry<>(node.key(), node.state().get()));
            }
            return node.state().isPresent() ? TraverseDecision.DO_NOT_GO_DEEPER : TraverseDecision.CONTINUE;
        });
        return result;
    }

    private @Nullable PrefixTreeImpl<Key, Value> findNode(List<Key> key) {
        PrefixTreeImpl<Key, Value> node = this;
        for (Key keyElement : key) {
            PrefixTreeImpl<Key, Value> child = node.myChildren.get(keyElement);
            if (child == null) {
                return null;
            }
            node = child;
        }
        return node;
    }

    private void traverseNode(List<Key> key, Consumer<TreeNode<Key, Value>> process) {
        PrefixTreeImpl<Key, Value> node = this;
        process.accept(new TreeNode<>(Collections.emptyList(), node.myState));
        for (int i = 0; i < key.size(); i++) {
            PrefixTreeImpl<Key, Value> child = node.myChildren.get(key.get(i));
            if (child == null) {
                return;
            }
            node = child;
            process.accept(new TreeNode<>(key.subList(0, i + 1), node.myState));
        }
    }

    private void traverseTree(Function<TreeNode<Key, Value>, TraverseDecision> process) {
        Deque<Pair<FList<Key>, PrefixTreeImpl<Key, Value>>> queue = new ArrayDeque<>();
        queue.addLast(Pair.create(FList.emptyList(), this));
        while (!queue.isEmpty()) {
            Pair<FList<Key>, PrefixTreeImpl<Key, Value>> item = queue.removeFirst();
            FList<Key> key = item.getFirst();
            PrefixTreeImpl<Key, Value> node = item.getSecond();
            TraverseDecision decision = process.apply(new TreeNode<>(asReversed(key), node.myState));
            if (decision == TraverseDecision.STOP) {
                break;
            }
            if (decision == TraverseDecision.DO_NOT_GO_DEEPER) {
                continue;
            }
            for (Map.Entry<Key, PrefixTreeImpl<Key, Value>> child : node.myChildren.entrySet()) {
                queue.addLast(Pair.create(key.prepend(child.getKey()), child.getValue()));
            }
        }
    }

    private static <Key> List<Key> asReversed(FList<Key> key) {
        List<Key> result = new ArrayList<>(key);
        Collections.reverse(result);
        return result;
    }

    private enum TraverseDecision {
        CONTINUE,
        STOP,
        DO_NOT_GO_DEEPER
    }

    private record TreeNode<Key, Value extends @Nullable Object>(List<Key> key, NodeState<Value> state) {
    }

    private static final class TreeEntry<Key, Value extends @Nullable Object> extends SimpleEntry<List<Key>, Value> {
        private TreeEntry(List<Key> key, Value value) {
            super(key, value);
        }
    }

    private static final class NodeState<T extends @Nullable Object> {
        private static final NodeState<?> EMPTY = new NodeState<>(false, null);

        private final boolean myPresent;

        private final @Nullable T myValue;

        private NodeState(boolean present, @Nullable T value) {
            myPresent = present;
            myValue = value;
        }

        @SuppressWarnings("unchecked")
        private static <T extends @Nullable Object> NodeState<T> empty() {
            return (NodeState<T>) EMPTY;
        }

        private static <T extends @Nullable Object> NodeState<T> of(T value) {
            return new NodeState<>(true, value);
        }

        private boolean isPresent() {
            return myPresent;
        }

        private T get() {
            if (myPresent) {
                return myValue;
            }
            throw new NoSuchElementException("No value present");
        }

        private @Nullable T getOrNull() {
            return myPresent ? myValue : null;
        }

        private T getOrDefault(T defaultValue) {
            if (myPresent) {
                return get();
            }
            return defaultValue;
        }
    }
}
