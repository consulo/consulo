/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.containers;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * Makes some THashSet methods open to the public.
 * Adds {@link #get(Object)} method useful to intern objects.
 *
 * @author gregsh
 */
@Deprecated
public class OpenTHashSet<T> extends THashSet<T> {
  public OpenTHashSet() {
    super();
  }

  public OpenTHashSet(final TObjectHashingStrategy<T> strategy) {
    super(strategy);
  }

  public OpenTHashSet(final int initialCapacity) {
    super(initialCapacity);
  }

  public OpenTHashSet(final int initialCapacity, final TObjectHashingStrategy<T> strategy) {
    super(initialCapacity, strategy);
  }

  public OpenTHashSet(final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public OpenTHashSet(final int initialCapacity, final float loadFactor, final TObjectHashingStrategy<T> strategy) {
    super(initialCapacity, loadFactor, strategy);
  }

  public OpenTHashSet(final Collection<? extends T> ts) {
    super(ts);
  }

  public OpenTHashSet(final Collection<? extends T> ts, final TObjectHashingStrategy<T> strategy) {
    super(ts, strategy);
  }

  public OpenTHashSet(final TObjectHashingStrategy<T> strategy, final T... ts) {
    super(Arrays.asList(ts), strategy);
  }

  @Override
  public int index(final T obj) {
    return super.index(obj);
  }

  public T get(final int index) {
    @SuppressWarnings("unchecked") T t = (T)_set[index];
    return t;
  }

  /**
   * Returns an element of this set equal to the given one. Can be used for interning objects to save memory.
   */
  @Nullable
  public T get(final T obj) {
    final int index = index(obj);
    return index < 0 ? null : get(index);
  }
}
