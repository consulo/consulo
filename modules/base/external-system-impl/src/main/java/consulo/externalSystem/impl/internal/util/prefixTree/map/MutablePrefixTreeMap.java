// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.map;

import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

/**
 * Mutable interface for {@link PrefixTreeMap}.
 *
 * @see PrefixTreeMap
 */
public interface MutablePrefixTreeMap<Key, Value extends @Nullable Object> extends PrefixTreeMap<Key, Value> {

    @Override
    @Nullable Value put(Key key, Value value);

    @Override
    @Nullable Value remove(Object key);

    default @Nullable Value set(Key key, Value value) {
        return put(key, value);
    }

    default void putAll(Iterable<? extends Pair<Key, Value>> entries) {
        for (Pair<Key, Value> entry : entries) {
            put(entry.getFirst(), entry.getSecond());
        }
    }
}
