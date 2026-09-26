// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.map;

import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPrefixTreeMap<Key, Value extends @Nullable Object> extends AbstractMap<Key, Value>
    implements PrefixTreeMap<Key, Value> {

    @Override
    public final Set<Key> getAncestorKeys(Key key) {
        return toKeySet(getAncestorEntries(key));
    }

    @Override
    public final List<Value> getAncestorValues(Key key) {
        return toValueList(getAncestorEntries(key));
    }

    @Override
    public final Set<Key> getDescendantKeys(Key key) {
        return toKeySet(getDescendantEntries(key));
    }

    @Override
    public final List<Value> getDescendantValues(Key key) {
        return toValueList(getDescendantEntries(key));
    }

    @Override
    public final Set<Key> getRootKeys() {
        return toKeySet(getRootEntries());
    }

    @Override
    public final List<Value> getRootValues() {
        return toValueList(getRootEntries());
    }

    private Set<Key> toKeySet(Iterable<Map.Entry<Key, Value>> entries) {
        Set<Key> result = new LinkedHashSet<>();
        for (Map.Entry<Key, Value> entry : entries) {
            result.add(entry.getKey());
        }
        return result;
    }

    private List<Value> toValueList(Iterable<Map.Entry<Key, Value>> entries) {
        List<Value> result = new ArrayList<>();
        for (Map.Entry<Key, Value> entry : entries) {
            result.add(entry.getValue());
        }
        return result;
    }
}
