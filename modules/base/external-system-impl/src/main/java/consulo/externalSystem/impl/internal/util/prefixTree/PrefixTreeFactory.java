// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import consulo.externalSystem.impl.internal.util.prefixTree.map.MutablePrefixTreeMap;
import consulo.externalSystem.impl.internal.util.prefixTree.map.PrefixTreeMapImpl;
import consulo.externalSystem.impl.internal.util.prefixTree.set.MutablePrefixTreeSet;
import consulo.externalSystem.impl.internal.util.prefixTree.set.PrefixTreeSetImpl;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface PrefixTreeFactory<Key, KeyElement> {

    List<KeyElement> convertToList(Key element);

    default MutablePrefixTreeSet<Key> createSet() {
        return new PrefixTreeSetImpl<>(this);
    }

    default <Value extends @Nullable Object> MutablePrefixTreeMap<Key, Value> createMap() {
        return new PrefixTreeMapImpl<>(this);
    }

    static <K, E> PrefixTreeFactory<K, E> create(Function<K, List<E>> convert) {
        return new PrefixTreeFactory<>() {
            @Override
            public List<E> convertToList(K element) {
                return convert.apply(element);
            }
        };
    }
}
