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

import java.util.function.Function;

public final class HashingUtil {
    private HashingUtil() {
    }

    public static <T, K> HashingStrategy<T> mappingStrategy(Function<T, K> keyExtractor) {
        return mappingStrategy(keyExtractor, HashingStrategy.canonical());
    }

    public static <T, K> HashingStrategy<T> mappingStrategy(Function<T, K> keyExtractor, HashingStrategy<K> keyStrategy) {
        return new MappingHashingStrategy<>(keyStrategy, keyExtractor);
    }
}
