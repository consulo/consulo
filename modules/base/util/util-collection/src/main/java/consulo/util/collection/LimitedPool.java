/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;

/*
 * @author max
 */
public class LimitedPool<T> {
  private final int capacity;
  private final ObjectFactory<T> factory;
  private Object[] storage;
  private int index = 0;

  public LimitedPool(final int capacity, ObjectFactory<T> factory) {
    this.capacity = capacity;
    this.factory = factory;
    storage = new Object[10];
  }

  public interface ObjectFactory<T> {
    T create();
    void cleanup(T t);
  }

  public T alloc() {
    if (index == 0) return factory.create();
    int i = --index;
    //noinspection unchecked
    T result = (T)storage[i];
    storage[i] = null;
    return result;
  }

  public void recycle(@Nonnull T t) {
    factory.cleanup(t);

    if (index >= capacity) return;

    ensureCapacity();
    storage[index++] = t;
  }

  private void ensureCapacity() {
    if (storage.length <= index) {
      int newCapacity = Math.min(capacity, storage.length * 3 / 2);
      Object[] newStorage = new Object[newCapacity];
      System.arraycopy(storage, 0, newStorage, 0, storage.length);
      storage = newStorage;
    }
  }
}
