// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.lang.IncorrectOperationException;
import consulo.util.lang.ref.SoftReference;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.function.Supplier;

//@Debug.Renderer(text = "\"size = \" + size()", hasChildren = "!isEmpty()", childrenArray = "childrenArray()")
public abstract class RefValueHashMap<K, V> implements Map<K, V> {
  private final Map<K, MyReference<K, V>> myMap;
  private final ReferenceQueue<V> myQueue = new ReferenceQueue<>();

  @Nonnull
  public static IncorrectOperationException pointlessContainsKey() {
    return new IncorrectOperationException("containsKey() makes no sense for weak/soft map because GC can clear the value any moment now");
  }

  @Nonnull
  public static IncorrectOperationException pointlessContainsValue() {
    return new IncorrectOperationException("containsValue() makes no sense for weak/soft map because GC can clear the key any moment now");
  }

  protected interface MyReference<K, T> extends Supplier<T> {
    @Nonnull
    K getKey();
  }

  RefValueHashMap() {
    myMap = Maps.newHashMap(HashingStrategy.canonical());
  }

  RefValueHashMap(@Nonnull HashingStrategy<K> strategy) {
    myMap = Maps.newHashMap(strategy);
  }

  protected abstract MyReference<K, V> createReference(@Nonnull K key, V value, @Nonnull ReferenceQueue<? super V> queue);

  private void processQueue() {
    while (true) {
      //noinspection unchecked
      MyReference<K, V> ref = (MyReference<K, V>)myQueue.poll();
      if (ref == null) {
        return;
      }
      K key = ref.getKey();
      if (myMap.get(key) == ref) {
        myMap.remove(key);
      }
    }
  }

  @Override
  public V get(Object key) {
    MyReference<K, V> ref = myMap.get(key);
    return SoftReference.deref(ref);
  }

  @Override
  public V put(@Nonnull K key, V value) {
    processQueue();
    MyReference<K, V> reference = createReference(key, value, myQueue);
    MyReference<K, V> oldRef = myMap.put(key, reference);
    return SoftReference.deref(oldRef);
  }

  @Override
  public V remove(Object key) {
    processQueue();
    MyReference<K, V> ref = myMap.remove(key);
    return SoftReference.deref(ref);
  }

  @Override
  public void putAll(@Nonnull Map<? extends K, ? extends V> t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    myMap.clear();
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
    throw pointlessContainsKey();
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
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
    final Collection<MyReference<K, V>> refs = myMap.values();
    for (MyReference<K, V> ref : refs) {
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

  @SuppressWarnings("unused")
  // used in debugger renderer
  private Map.Entry[] childrenArray() {
    return myMap.entrySet().stream().map(entry -> {
      Object val = SoftReference.deref(entry.getValue());
      return val != null ? new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), val) : null;
    }).filter(Objects::nonNull).toArray(Entry[]::new);
  }
}
