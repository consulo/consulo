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
package consulo.util.collection.primitive.ints;

import consulo.annotation.DeprecationInfo;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public interface IntObjectMap<V> extends Map<Integer, V> {
  /**
   * @return old value by key
   */
  @Nullable
  V put(int key, V value);

  @Nullable
  V get(int key);

  boolean containsKey(int key);

  default V putIfAbsent(int key, V value) {
    V v = get(key);
    if (v == null) {
      v = put(key, value);
    }

    return v;
  }

  @Override
  @Deprecated
  @DeprecationInfo("Use unboxed version of #containsKey")
  default V put(Integer key, V value) {
    return put(((Integer)key).intValue(), value);
  }

  @Override
  @Deprecated
  @DeprecationInfo("Use unboxed version of #containsKey")
  default boolean containsKey(Object key) {
    return containsKey(((Integer)key).intValue());
  }
}
