// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.map;

import consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeFactory;
import consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeImpl;
import org.jspecify.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PrefixTreeMapImpl<Key, KeyElement, Value extends @Nullable Object> extends AbstractPrefixTreeMap<Key, Value>
    implements MutablePrefixTreeMap<Key, Value> {

    private final PrefixTreeFactory<Key, KeyElement> myConvertor;

    private final PrefixTreeImpl<KeyElement, SimpleEntry<Key, Value>> myTree = new PrefixTreeImpl<>();

    public PrefixTreeMapImpl(PrefixTreeFactory<Key, KeyElement> convertor) {
        myConvertor = convertor;
    }

    @Override
    public int size() {
        return myTree.size();
    }

    @Override
    public Set<Map.Entry<Key, Value>> entrySet() {
        return new LinkedHashSet<>(myTree.values());
    }

    @Override
    public @Nullable Value get(Object key) {
        SimpleEntry<Key, Value> entry = myTree.get(toList(key));
        return entry != null ? entry.getValue() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Value getOrDefault(Object key, Value defaultValue) {
        return myTree.getOrDefault(toList(key), new SimpleEntry<>((Key) key, defaultValue)).getValue();
    }

    @Override
    public @Nullable Value put(Key key, Value value) {
        SimpleEntry<Key, Value> previous = myTree.put(toList(key), new SimpleEntry<>(key, value));
        return previous != null ? previous.getValue() : null;
    }

    @Override
    public @Nullable Value remove(Object key) {
        SimpleEntry<Key, Value> previous = myTree.remove(toList(key));
        return previous != null ? previous.getValue() : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return myTree.containsKey(toList(key));
    }

    @Override
    public Set<Map.Entry<Key, Value>> getDescendantEntries(Key key) {
        return new LinkedHashSet<>(myTree.getDescendantValues(toList(key)));
    }

    @Override
    public Set<Map.Entry<Key, Value>> getAncestorEntries(Key key) {
        return new LinkedHashSet<>(myTree.getAncestorValues(toList(key)));
    }

    @Override
    public Set<Map.Entry<Key, Value>> getRootEntries() {
        return new LinkedHashSet<>(myTree.getRootValues());
    }

    @SuppressWarnings("unchecked")
    private List<KeyElement> toList(Object key) {
        return myConvertor.convertToList((Key) key);
    }
}
