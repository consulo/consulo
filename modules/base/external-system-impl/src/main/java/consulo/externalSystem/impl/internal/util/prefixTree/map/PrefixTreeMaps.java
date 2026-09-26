// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.map;

import consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeFactory;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class PrefixTreeMaps {

    private PrefixTreeMaps() {
    }

    /**
     * Returns a new {@link PrefixTreeMap} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    public static <K, E, V extends @Nullable Object> PrefixTreeMap<K, V> toPrefixTreeMap(
        Iterable<? extends Pair<K, V>> entries,
        PrefixTreeFactory<K, E> factory
    ) {
        return toMutablePrefixTreeMap(entries, factory);
    }

    /**
     * Returns a new {@link PrefixTreeMap} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    @SafeVarargs
    public static <K, E, V extends @Nullable Object> PrefixTreeMap<K, V> asMap(PrefixTreeFactory<K, E> factory, Pair<K, V>... entries) {
        return asMutableMap(factory, entries);
    }

    /**
     * Returns a new {@link MutablePrefixTreeMap} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    public static <K, E, V extends @Nullable Object> MutablePrefixTreeMap<K, V> toMutablePrefixTreeMap(
        Iterable<? extends Pair<K, V>> entries,
        PrefixTreeFactory<K, E> factory
    ) {
        MutablePrefixTreeMap<K, V> map = factory.createMap();
        map.putAll(entries);
        return map;
    }

    /**
     * Returns a new {@link MutablePrefixTreeMap} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    @SafeVarargs
    public static <K, E, V extends @Nullable Object> MutablePrefixTreeMap<K, V> asMutableMap(
        PrefixTreeFactory<K, E> factory,
        Pair<K, V>... entries
    ) {
        return toMutablePrefixTreeMap(Arrays.asList(entries), factory);
    }
}
