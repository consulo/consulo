// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.util;

import consulo.util.collection.HashingStrategy;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

final class MappingHashingStrategy<T, K> implements HashingStrategy<T> {
    private final HashingStrategy<K> myKeyStrategy;
    private final Function<T, K> myKeyExtractor;

    MappingHashingStrategy(HashingStrategy<K> keyStrategy, Function<T, K> keyExtractor) {
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
