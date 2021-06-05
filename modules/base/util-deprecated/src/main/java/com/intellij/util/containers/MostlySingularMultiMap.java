/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.util.containers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

public class MostlySingularMultiMap<K, V> implements Serializable {
  private static final long serialVersionUID = 2784473565881807109L;

  protected final Map<K, Object> myMap;

  public MostlySingularMultiMap() {
    myMap = createMap();
  }

  @Nonnull
  protected Map<K, Object> createMap() {
    return new HashMap<K, Object>();
  }

  public void add(@Nonnull K key, @Nonnull V value) {
    Object current = myMap.get(key);
    if (current == null) {
      myMap.put(key, value);
    }
    else if (current instanceof MostlySingularMultiMap.ValueList) {
      //noinspection unchecked
      ValueList<Object> curList = (ValueList<Object>)current;
      curList.add(value);
    }
    else {
      ValueList<Object> newList = new ValueList<Object>();
      newList.add(current);
      newList.add(value);
      myMap.put(key, newList);
    }
  }

  public boolean remove(@Nonnull K key, @Nonnull V value) {
    Object current = myMap.get(key);
    if (current == null) {
      return false;
    }
    if (current instanceof MostlySingularMultiMap.ValueList) {
      ValueList curList = (ValueList)current;
      return curList.remove(value);
    }

    if (value.equals(current)) {
      myMap.remove(key);
      return true;
    }

    return false;
  }

  public boolean removeAllValues(@Nonnull K key) {
    return myMap.remove(key) != null;
  }

  @Nonnull
  public Set<K> keySet() {
    return myMap.keySet();
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  public boolean processForKey(@Nonnull K key, @Nonnull Processor<? super V> p) {
    return processValue(p, myMap.get(key));
  }

  @SuppressWarnings("unchecked")
  private boolean processValue(@Nonnull Processor<? super V> p, Object v) {
    if (v instanceof MostlySingularMultiMap.ValueList) {
      for (Object o : (ValueList)v) {
        if (!p.process((V)o)) return false;
      }
    }
    else if (v != null) {
      return p.process((V)v);
    }

    return true;
  }

  public boolean processAllValues(@Nonnull Processor<? super V> p) {
    for (Object v : myMap.values()) {
      if (!processValue(p, v)) return false;
    }

    return true;
  }

  public int size() {
    return myMap.size();
  }

  public boolean containsKey(@Nonnull K key) {
    return myMap.containsKey(key);
  }

  public int valuesForKey(@Nonnull K key) {
    Object current = myMap.get(key);
    if (current == null) return 0;
    if (current instanceof MostlySingularMultiMap.ValueList) return ((ValueList)current).size();
    return 1;
  }

  @Nonnull
  public Iterable<V> get(@Nonnull K name) {
    final Object value = myMap.get(name);
    return rawValueToCollection(value);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  protected List<V> rawValueToCollection(Object value) {
    if (value == null) return Collections.emptyList();

    if (value instanceof MostlySingularMultiMap.ValueList) {
      return (ValueList<V>)value;
    }

    return Collections.singletonList((V)value);
  }

  public void compact() {
    // FIXME [VISTALL] unsupported ((HashMap)myMap).trimToSize();
    for (Object eachValue : myMap.values()) {
      if (eachValue instanceof MostlySingularMultiMap.ValueList) {
        ((ValueList)eachValue).trimToSize();
      }
    }
  }

  @Override
  public String toString() {
    return "{" + StringUtil.join(myMap.entrySet(), new Function<Map.Entry<K, Object>, String>() {
      @Override
      public String fun(Map.Entry<K, Object> entry) {
        Object value = entry.getValue();
        String s = (value instanceof MostlySingularMultiMap.ValueList ? ((ValueList)value) : Collections.singletonList(value)).toString();
        return entry.getKey() + ": " + s;
      }
    }, "; ") + "}";
  }

  public void clear() {
    myMap.clear();
  }

  @Nonnull
  public static <K, V> MostlySingularMultiMap<K, V> emptyMap() {
    //noinspection unchecked
    return EMPTY;
  }

  @Nonnull
  public static <K, V> MostlySingularMultiMap<K, V> newMap() {
    return new MostlySingularMultiMap<K, V>();
  }

  private static final MostlySingularMultiMap EMPTY = new EmptyMap();

  @SuppressWarnings("unchecked")
  public void addAll(MostlySingularMultiMap<K, V> other) {
    if (other instanceof EmptyMap) return;

    for (Map.Entry<K, Object> entry : other.myMap.entrySet()) {
      K key = entry.getKey();
      Object otherValue = entry.getValue();
      Object myValue = myMap.get(key);

      if (myValue == null) {
        if (otherValue instanceof MostlySingularMultiMap.ValueList) {
          myMap.put(key, new ValueList((ValueList)otherValue));
        }
        else {
          myMap.put(key, otherValue);
        }
      }
      else if (myValue instanceof MostlySingularMultiMap.ValueList) {
        ValueList myListValue = (ValueList)myValue;
        if (otherValue instanceof MostlySingularMultiMap.ValueList) {
          myListValue.addAll((ValueList)otherValue);
        }
        else {
          myListValue.add(otherValue);
        }
      }
      else {
        if (otherValue instanceof MostlySingularMultiMap.ValueList) {
          ValueList otherListValue = (ValueList)otherValue;
          ValueList newList = new ValueList(otherListValue.size() + 1);
          newList.add(myValue);
          newList.addAll(otherListValue);
          myMap.put(key, newList);
        }
        else {
          ValueList newList = new ValueList();
          newList.add(myValue);
          newList.add(otherValue);
          myMap.put(key, newList);
        }
      }
    }
  }

  // marker class to distinguish multi-values from single values in case client want to store collections as values.
  protected static class ValueList<V> extends ArrayList<V> {
    public ValueList() {
    }

    public ValueList(int initialCapacity) {
      super(initialCapacity);
    }

    public ValueList(@Nonnull Collection<? extends V> c) {
      super(c);
    }
  }

  private static class EmptyMap extends MostlySingularMultiMap {
    @Override
    public void add(@Nonnull Object key, @Nonnull Object value) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean remove(@Nonnull Object key, @Nonnull Object value) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean removeAllValues(@Nonnull Object key) {
      throw new IncorrectOperationException();
    }

    @Override
    public void clear() {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public Set keySet() {
      return Collections.emptySet();
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean processForKey(@Nonnull Object key, @Nonnull Processor p) {
      return true;
    }

    @Override
    public boolean processAllValues(@Nonnull Processor p) {
      return true;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public int valuesForKey(@Nonnull Object key) {
      return 0;
    }

    @Nonnull
    @Override
    public Iterable get(@Nonnull Object name) {
      return ContainerUtil.emptyList();
    }
  }
}
