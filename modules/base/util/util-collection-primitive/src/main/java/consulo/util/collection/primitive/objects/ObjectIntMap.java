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
package consulo.util.collection.primitive.objects;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * @author VISTALL
 * @since 10/02/2021
 */
public interface ObjectIntMap<K> {
  interface Entry<T> {
    T getKey();

    int getValue();
  }

  int getInt(K key);

  void putInt(K key, int value);

  void putAll(@Nonnull ObjectIntMap<? extends K> map);

  int size();

  void clear();

  void forEach(ObjIntConsumer<? super K> action);

  boolean containsKey(K key);

  Set<Entry<K>> entrySet();
}
