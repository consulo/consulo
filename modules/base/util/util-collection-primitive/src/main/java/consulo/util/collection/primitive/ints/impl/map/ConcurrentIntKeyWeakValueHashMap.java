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

package consulo.util.collection.primitive.ints.impl.map;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Concurrent key:int -> weak value:V map
 * Null values are NOT allowed
 * Use {@link ContainerUtil#createConcurrentIntObjectWeakValueMap()} to create this
 */
public class ConcurrentIntKeyWeakValueHashMap<V> extends ConcurrentIntKeyRefValueHashMap<V> {
  private static class MyRef<V> extends WeakReference<V> implements IntReference<V> {
    private final int hash;
    private final int key;

    private MyRef(int key, @Nonnull V referent, @Nonnull ReferenceQueue<V> queue) {
      super(referent, queue);
      this.key = key;
      hash = referent.hashCode();
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      V v = get();
      if (!(obj instanceof MyRef)) {
        return false;
      }
      //noinspection unchecked
      MyRef<V> other = (MyRef<V>)obj;
      return other.hash == hash && key == other.getKey() && Comparing.equal(v, other.get());
    }

    @Override
    public int getKey() {
      return key;
    }
  }

  @Nonnull
  @Override
  protected IntReference<V> createReference(int key, @Nonnull V value, @Nonnull ReferenceQueue<V> queue) {
    return new MyRef<>(key, value, queue);
  }
}
