/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.document.impl;

import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.function.IntFunction;

/**
 * Maintains an atomic immutable array of listeners of type {@code T} in sorted order according to {@link #comparator}
 * N.B. internal array is exposed for faster iterating listeners in to- and reverse order, so care should be taken for not mutating it by clients
 */
class LockFreeCOWSortedArray<T> {
  @Nonnull
  private final Comparator<? super T> comparator;
  private final IntFunction<T[]> arrayFactory;
  /** changed by {@link #UPDATER} only */
  @SuppressWarnings("FieldMayBeFinal")
  @Nonnull
  private volatile T[] listeners;
  
  private static  VarHandle UPDATER;

  static {
    try {
      UPDATER = MethodHandles.lookup().findVarHandle(LockFreeCOWSortedArray.class, "listeners", Object[].class);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  LockFreeCOWSortedArray(@Nonnull Comparator<? super T> comparator, @Nonnull IntFunction<T[]> arrayFactory) {
    this.comparator = comparator;
    this.arrayFactory = arrayFactory;
    listeners = arrayFactory.apply(0);
  }

  // returns true if changed
  void add(@Nonnull T listener) {
    while (true) {
      T[] oldListeners = listeners;
      int i = insertionIndex(oldListeners, listener);
      T[] newListeners = ArrayUtil.insert(oldListeners, i, listener);
      if (UPDATER.compareAndSet(this, oldListeners, newListeners)) break;
    }
  }

  boolean remove(@Nonnull T listener) {
    while (true) {
      T[] oldListeners = listeners;
      T[] newListeners = ArrayUtil.remove(oldListeners, listener, arrayFactory);
      //noinspection ArrayEquality
      if (oldListeners == newListeners) return false;
      if (UPDATER.compareAndSet(this, oldListeners, newListeners)) break;
    }
    return true;
  }

  private int insertionIndex(@Nonnull T[] elements, @Nonnull T e) {
    for (int i=0; i<elements.length; i++) {
      T element = elements[i];
      if (comparator.compare(e, element) < 0) {
        return i;
      }
    }
    return elements.length;
  }

  @Nonnull
  T[] getArray() {
    return listeners;
  }
}
