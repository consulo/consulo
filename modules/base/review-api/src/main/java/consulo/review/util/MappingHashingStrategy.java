// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.util;

import consulo.util.collection.HashingStrategy;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

class MappingHashingStrategy<T, K> implements HashingStrategy<T> {
    private final HashingStrategy<K> myKeyStrategy;
    private final Function<? super T, ? extends K> myKeyExtractor;

    MappingHashingStrategy(HashingStrategy<K> keyStrategy, Function<? super T, ? extends K> keyExtractor) {
        myKeyStrategy = keyStrategy;
        myKeyExtractor = keyExtractor;
    }

    @Override
    public int hashCode(@Nullable T value) {
        return myKeyStrategy.hashCode(value == null ? null : myKeyExtractor.apply(value));
    }

    @Override
    public boolean equals(@Nullable T o1, @Nullable T o2) {
        K mapped1 = o1 == null ? null : myKeyExtractor.apply(o1);
        K mapped2 = o2 == null ? null : myKeyExtractor.apply(o2);
        return myKeyStrategy.equals(mapped1, mapped2);
    }
}
