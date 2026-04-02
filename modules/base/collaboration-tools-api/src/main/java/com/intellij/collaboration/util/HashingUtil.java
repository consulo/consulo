// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.util.collection.HashingStrategy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

public final class HashingUtil {
    private HashingUtil() {
    }

    @Nonnull
    public static <T, K> HashingStrategy<T> mappingStrategy(
        @Nonnull Function<T, K> keyExtractor,
        @Nonnull HashingStrategy<K> keyStrategy
    ) {
        return new MappingHashingStrategy<>(keyStrategy, keyExtractor);
    }

    @Nonnull
    public static <T, K> HashingStrategy<T> mappingStrategy(@Nonnull Function<T, K> keyExtractor) {
        return mappingStrategy(keyExtractor, HashingStrategy.canonical());
    }

    private static final class MappingHashingStrategy<T, K> implements HashingStrategy<T> {
        private final HashingStrategy<K> keyStrategy;
        private final Function<T, K> keyExtractor;

        MappingHashingStrategy(@Nonnull HashingStrategy<K> keyStrategy, @Nonnull Function<T, K> keyExtractor) {
            this.keyStrategy = keyStrategy;
            this.keyExtractor = keyExtractor;
        }

        @Override
        public int hashCode(@Nullable T value) {
            return keyStrategy.hashCode(value != null ? keyExtractor.apply(value) : null);
        }

        @Override
        public boolean equals(@Nullable T o1, @Nullable T o2) {
            K mapped1 = o1 != null ? keyExtractor.apply(o1) : null;
            K mapped2 = o2 != null ? keyExtractor.apply(o2) : null;
            return keyStrategy.equals(mapped1, mapped2);
        }
    }
}
