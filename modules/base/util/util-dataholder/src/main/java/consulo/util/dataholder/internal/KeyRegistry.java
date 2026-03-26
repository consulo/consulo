/*
 * Copyright 2013-2017 consulo.io
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
package consulo.util.dataholder.internal;

import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2017-10-15
 */
public class KeyRegistry {
    public static final KeyRegistry ourInstance = new KeyRegistry();

    private final ConcurrentIntObjectMap<Key> myAllKeys = IntMaps.newConcurrentIntObjectWeakValueHashMap();
    private final AtomicInteger myKeyCounter = new AtomicInteger();

    private KeyRegistry() {
    }

    @SuppressWarnings("deprecation")
    public int register(Key<?> key) {
        int index = myKeyCounter.getAndIncrement();
        myAllKeys.put(index, key);
        return index;
    }

    public @Nullable Key<?> findKeyByName(String name, Function<Key<?>, String> nameFunc) {
        for (IntObjectMap.IntObjectEntry<Key> key : myAllKeys.entrySet()) {
            if (name.equals(nameFunc.apply(Objects.requireNonNull(key.getValue())))) {
                return key.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable Key<T> getKeyByIndex(int index) {
        return myAllKeys.get(index);
    }

    @SuppressWarnings("unchecked")
    public <T> Key<T> getRequiredKeyByIndex(int index) {
        @Nullable Key key = getKeyByIndex(index);
        if (key == null) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        return key;
    }
}
