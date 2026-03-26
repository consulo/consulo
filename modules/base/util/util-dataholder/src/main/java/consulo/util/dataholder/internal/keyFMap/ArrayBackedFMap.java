/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.util.dataholder.internal.keyFMap;

import consulo.util.dataholder.Key;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.internal.KeyRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ArrayBackedFMap implements KeyFMap {
    private static final KeyRegistry ourRegistry = KeyRegistry.ourInstance;

    static final int ARRAY_THRESHOLD = 8;
    private final int[] keys;
    private final Object[] values;

    ArrayBackedFMap(int[] keys, Object[] values) {
        this.keys = keys;
        this.values = values;
    }

    @Override
    public <V> KeyFMap plus(Key<V> key, V value) {
        int oldSize = size();
        int keyCode = key.hashCode();
        int[] newKeys = null;
        Object[] newValues = null;
        int i;
        for (i = 0; i < oldSize; i++) {
            int oldKey = keys[i];
            if (keyCode == oldKey) {
                if (value == values[i]) {
                    return this;
                }
                newKeys = new int[oldSize];
                newValues = new Object[oldSize];
                System.arraycopy(keys, 0, newKeys, 0, oldSize);
                System.arraycopy(values, 0, newValues, 0, oldSize);
                newValues[i] = value;
                break;
            }
        }
        if (i == oldSize) {
            if (oldSize == ARRAY_THRESHOLD) {
                return new MapBackedFMap(keys, keyCode, values, value);
            }
            newKeys = ArrayUtil.append(keys, keyCode);
            newValues = ArrayUtil.append(values, value, ArrayUtil.OBJECT_ARRAY_FACTORY);
        }
        //noinspection ConstantConditions
        return new ArrayBackedFMap(Objects.requireNonNull(newKeys), Objects.requireNonNull(newValues));
    }

    private int size() {
        return keys.length;
    }

    @Override
    public KeyFMap minus(Key<?> key) {
        int oldSize = size();
        int keyCode = key.hashCode();
        for (int i = 0; i < oldSize; i++) {
            int oldKey = keys[i];
            if (keyCode == oldKey) {
                if (oldSize == 3) {
                    int i1 = (2 - i) / 2;
                    int i2 = 3 - (i + 2) / 2;
                    Key<Object> key1 = ourRegistry.getKeyByIndex(keys[i1]);
                    Key<Object> key2 = ourRegistry.getKeyByIndex(keys[i2]);
                    if (key1 != null && key2 != null) {
                        return new PairElementsFMap(key1, values[i1], key2, values[i2]);
                    }
                    if (key1 != null) {
                        return new OneElementFMap<>(key1, values[i1]);
                    }
                    if (key2 != null) {
                        return new OneElementFMap<>(key2, values[i2]);
                    }
                    return EMPTY_MAP;
                }
                int[] newKeys = ArrayUtil.remove(keys, i);
                Object[] newValues = ArrayUtil.remove(values, i, ArrayUtil.OBJECT_ARRAY_FACTORY);
                return new ArrayBackedFMap(newKeys, newValues);
            }
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> @Nullable V get(Key<V> key) {
        int oldSize = size();
        int keyCode = key.hashCode();
        for (int i = 0; i < oldSize; i++) {
            int oldKey = keys[i];
            if (keyCode == oldKey) {
                return (V)values[i];
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("(");
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            Object value = values[i];
            if (!s.isEmpty()) {
                s.append(", ");
            }
            s.append(ourRegistry.getKeyByIndex(key)).append(" -> ").append(value);
        }
        return s.append(")").toString();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public int[] getKeyIds() {
        return keys;
    }

    @Override
    public Key[] getKeys() {
        return getKeysByIndices(keys);
    }

    public Object[] getValues() {
        return values;
    }

    static Key[] getKeysByIndices(int[] indexes) {
        Key[] result = new Key[indexes.length];

        for (int i = 0; i < indexes.length; i++) {
            result[i] = ourRegistry.getRequiredKeyByIndex(indexes[i]);
        }

        return result;
    }
}
