/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection;

import consulo.util.collection.impl.FastUtilHashingStrategies;
import consulo.util.collection.impl.map.ConcurrentHashMap;
import consulo.util.collection.impl.set.WeakHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2021-01-16
 */
public final class Sets {
    private static final int UNKNOWN_CAPACITY = -1;

    private static <T> Set<T> newHashSetWithStrategy(int capacity, @Nullable Collection<? extends T> inner, HashingStrategy<T> strategy) {
        if (inner != null) {
            return new ObjectOpenCustomHashSet<>(inner, FastUtilHashingStrategies.of(strategy));
        }
        else if (capacity == UNKNOWN_CAPACITY) {
            return new ObjectOpenCustomHashSet<>(FastUtilHashingStrategies.of(strategy));
        }
        else {
            return new ObjectOpenCustomHashSet<>(capacity, FastUtilHashingStrategies.of(strategy));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> notNullize(@Nullable Set<T> set) {
        return set == null ? Set.<T>of() : set;
    }

    public static <T> Set<T> newWeakHashSet() {
        return new WeakHashSet<>();
    }

    public static <T> Set<T> newHashSet(HashingStrategy<T> hashingStrategy) {
        return newHashSet(UNKNOWN_CAPACITY, hashingStrategy);
    }

    public static <T> Set<T> newHashSet(Collection<? extends T> items, HashingStrategy<T> hashingStrategy) {
        return newHashSetWithStrategy(UNKNOWN_CAPACITY, items, hashingStrategy);
    }

    public static <K> Set<K> newHashSet(int initialCapacity, HashingStrategy<K> hashingStrategy) {
        return newHashSetWithStrategy(initialCapacity, null, hashingStrategy);
    }

    public static <T> Set<T> newLinkedHashSet(HashingStrategy<T> hashingStrategy) {
        return Collections.newSetFromMap(Maps.newLinkedHashMap(hashingStrategy));
    }

    public static <K> Set<K> newIdentityHashSet() {
        return newHashSet(UNKNOWN_CAPACITY, HashingStrategy.identity());
    }

    public static <K> Set<K> newIdentityHashSet(int initialCapacity) {
        return newHashSet(initialCapacity, HashingStrategy.identity());
    }

    public static <T> Set<T> newConcurrentHashSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public static <T> Set<T> newConcurrentHashSet(HashingStrategy<T> hashStrategy) {
        return Collections.newSetFromMap(Maps.newConcurrentHashMap(hashStrategy));
    }
}
