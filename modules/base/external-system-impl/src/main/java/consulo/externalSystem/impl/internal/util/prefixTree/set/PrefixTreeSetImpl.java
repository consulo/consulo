// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.set;

import consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeFactory;
import consulo.externalSystem.impl.internal.util.prefixTree.map.MutablePrefixTreeMap;
import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public final class PrefixTreeSetImpl<Key, KeyElement> extends AbstractSet<Key> implements MutablePrefixTreeSet<Key> {

    private final MutablePrefixTreeMap<Key, @Nullable Void> myMap;

    public PrefixTreeSetImpl(PrefixTreeFactory<Key, KeyElement> convertor) {
        myMap = convertor.createMap();
    }

    @Override
    public int size() {
        return myMap.size();
    }

    @Override
    public boolean contains(Object element) {
        return myMap.containsKey(element);
    }

    @Override
    public Set<Key> getDescendants(Key element) {
        return myMap.getDescendantKeys(element);
    }

    @Override
    public Set<Key> getAncestors(Key element) {
        return myMap.getAncestorKeys(element);
    }

    @Override
    public Set<Key> getRoots() {
        return myMap.getRootKeys();
    }

    @Override
    public boolean add(Key element) {
        int previousSize = myMap.size();
        myMap.set(element, null);
        return myMap.size() != previousSize;
    }

    @Override
    public boolean remove(Object element) {
        int previousSize = myMap.size();
        myMap.remove(element);
        return myMap.size() != previousSize;
    }

    @Override
    public Iterator<Key> iterator() {
        return myMap.keySet().iterator();
    }
}
