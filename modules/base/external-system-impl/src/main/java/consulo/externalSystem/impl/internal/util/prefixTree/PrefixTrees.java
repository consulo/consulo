// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class PrefixTrees {

    private PrefixTrees() {
    }

    /**
     * Returns a new {@link PrefixTree} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    public static <K, V extends @Nullable Object> PrefixTree<K, V> toPrefixTree(Iterable<? extends Pair<List<K>, V>> entries) {
        return toMutablePrefixTree(entries);
    }

    /**
     * Returns a new {@link PrefixTree} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    @SafeVarargs
    public static <K, V extends @Nullable Object> PrefixTree<K, V> prefixTreeOf(Pair<List<K>, V>... entries) {
        return mutablePrefixTreeOf(entries);
    }

    /**
     * Returns an empty {@link PrefixTree} of specified type.
     */
    public static <K, V extends @Nullable Object> PrefixTree<K, V> emptyPrefixTree() {
        return new PrefixTreeImpl<>();
    }

    /**
     * Returns a new {@link MutablePrefixTree} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    public static <K, V extends @Nullable Object> MutablePrefixTree<K, V> toMutablePrefixTree(
        Iterable<? extends Pair<List<K>, V>> entries
    ) {
        MutablePrefixTree<K, V> tree = new PrefixTreeImpl<>();
        tree.putAll(entries);
        return tree;
    }

    /**
     * Returns a new {@link MutablePrefixTree} with the specified contents, given as a list of pairs
     * where the first value is the key and the second is the value.
     * <p>
     * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
     */
    @SafeVarargs
    public static <K, V extends @Nullable Object> MutablePrefixTree<K, V> mutablePrefixTreeOf(Pair<List<K>, V>... entries) {
        return toMutablePrefixTree(Arrays.asList(entries));
    }
}
