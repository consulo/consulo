// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.set;

import consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeFactory;

import java.util.Arrays;

public final class PrefixTreeSets {

    private PrefixTreeSets() {
    }

    /**
     * Returns a new {@link PrefixTreeSet} containing all distinct elements from the given collection.
     */
    public static <K, E> PrefixTreeSet<K> toPrefixTreeSet(Iterable<? extends K> elements, PrefixTreeFactory<K, E> factory) {
        return toMutablePrefixTreeSet(elements, factory);
    }

    /**
     * Returns a new {@link PrefixTreeSet} containing all distinct elements from the given collection.
     */
    @SafeVarargs
    public static <K, E> PrefixTreeSet<K> asSet(PrefixTreeFactory<K, E> factory, K... elements) {
        return asMutableSet(factory, elements);
    }

    /**
     * Returns a new {@link MutablePrefixTreeSet} containing all distinct elements from the given collection.
     */
    public static <K, E> MutablePrefixTreeSet<K> toMutablePrefixTreeSet(Iterable<? extends K> elements, PrefixTreeFactory<K, E> factory) {
        MutablePrefixTreeSet<K> set = factory.createSet();
        set.addAll(elements);
        return set;
    }

    /**
     * Returns a new {@link MutablePrefixTreeSet} containing all distinct elements from the given collection.
     */
    @SafeVarargs
    public static <K, E> MutablePrefixTreeSet<K> asMutableSet(PrefixTreeFactory<K, E> factory, K... elements) {
        return toMutablePrefixTreeSet(Arrays.asList(elements), factory);
    }
}
