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

import consulo.util.collection.primitive.ints.IntCollection;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * @author VISTALL
 * @since 22/05/2021
 */
class UnmodifiedObjectIntMap<K> implements ObjectIntMap<K> {
  private final ObjectIntMap<K> myDelegate;

  UnmodifiedObjectIntMap(ObjectIntMap<K> delegate) {
    myDelegate = delegate;
  }

  @Override
  public int getInt(K key) {
    return myDelegate.getInt(key);
  }

  @Override
  public int getIntOrDefault(K key, int defaultValue) {
    return myDelegate.getIntOrDefault(key, defaultValue);
  }

  @Override
  public void putInt(K key, int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(@Nonnull ObjectIntMap<? extends K> map) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return myDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEach(ObjIntConsumer<? super K> action) {
    myDelegate.forEach(action);
  }

  @Override
  public boolean containsKey(K key) {
    return myDelegate.containsKey(key);
  }

  @Override
  public int remove(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Entry<K>> entrySet() {
    return Collections.unmodifiableSet(myDelegate.entrySet());
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(myDelegate.keySet());
  }

  @Override
  public IntCollection values() {
    return myDelegate.values();
  }
}
