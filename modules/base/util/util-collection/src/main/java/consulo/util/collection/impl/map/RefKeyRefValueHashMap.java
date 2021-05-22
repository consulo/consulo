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
package consulo.util.collection.impl.map;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.util.*;

public abstract class RefKeyRefValueHashMap<K, V> implements Map<K, V> {
  private final RefHashMap<K, ValueReference<K, V>> myMap;
  private final ReferenceQueue<V> myQueue = new ReferenceQueue<>();

  public RefKeyRefValueHashMap(@Nonnull RefHashMap<K, ValueReference<K, V>> weakKeyMap) {
    myMap = weakKeyMap;
  }

  protected interface ValueReference<K, V> {
    @Nonnull
    RefHashMap.Key<K> getKey();

    V get();
  }

  protected V dereference(ValueReference<K, ? extends V> reference) {
    return reference == null ? null : reference.get();
  }

  @Nonnull
  protected abstract ValueReference<K, V> createValueReference(@Nonnull RefHashMap.Key<K> key, V referent, ReferenceQueue<? super V> q);

  // returns true if some refs were tossed
  boolean processQueue() {
    boolean processed = myMap.processQueue();
    while (true) {
      ValueReference<K, V> ref = (ValueReference<K, V>)myQueue.poll();
      if (ref == null) break;
      RefHashMap.Key<K> weakKey = ref.getKey();
      myMap.removeKey(weakKey);
      processed = true;
    }
    return processed;
  }

  @Override
  public V get(Object key) {
    ValueReference<K, V> ref = myMap.get(key);
    return dereference(ref);
  }

  @Override
  public V put(K key, V value) {
    processQueue();
    RefHashMap.Key<K> weakKey = myMap.createKey(key);
    ValueReference<K, V> reference = createValueReference(weakKey, value, myQueue);
    ValueReference<K, V> oldRef = myMap.putKey(weakKey, reference);
    return dereference(oldRef);
  }

  @Override
  public V remove(Object key) {
    processQueue();
    ValueReference<K, V> ref = myMap.remove(key);
    return dereference(ref);
  }

  @Override
  public void putAll(@Nonnull Map<? extends K, ? extends V> t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
  }

  @Override
  public int size() {
    return myMap.size(); //?
  }

  @Override
  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    throw RefValueHashMap.pointlessContainsKey();
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Nonnull
  @Override
  public Set<K> keySet() {
    return myMap.keySet();
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    List<V> result = new ArrayList<>();
    final Collection<ValueReference<K, V>> refs = myMap.values();
    for (ValueReference<K, V> ref : refs) {
      final V value = ref.get();
      if (value != null) {
        result.add(value);
      }
    }
    return result;
  }

  @Nonnull
  @Override
  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }
}
