// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.util;

import consulo.util.collection.HashingStrategy;

import java.util.function.Function;

public final class HashingUtil {
    private HashingUtil() {
    }

    public static <T, K> HashingStrategy<T> mappingStrategy(Function<? super T, ? extends K> keyExtractor) {
        return mappingStrategy(keyExtractor, HashingStrategy.canonical());
    }

    public static <T, K> HashingStrategy<T> mappingStrategy(Function<? super T, ? extends K> keyExtractor, HashingStrategy<K> keyStrategy) {
        return new MappingHashingStrategy<>(keyStrategy, keyExtractor);
    }
}
