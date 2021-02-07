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
package consulo.util.collection.fastutil.impl.ints;

import consulo.util.collection.primitive.ints.IntObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Map;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class FHashIntObjectMap<V> extends Int2ObjectOpenHashMap<V> implements IntObjectMap<V> {
  @Override
  public ObjectSet<Map.Entry<Integer, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V put(Integer key, V value) {
    return super.put(key, value);
  }

  @Override
  public boolean containsKey(Object key) {
    return super.containsKey(key);
  }
}
