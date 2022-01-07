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

package com.intellij.openapi.util;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author peter
 */
public abstract class AtomicNotNullLazyValue<T> extends NotNullLazyValue<T> {
  @Nonnull
  public static <K> AtomicNotNullLazyValue<K> createValue(@Nonnull final NotNullFactory<K> value) {
    return new AtomicNotNullLazyValue<K>() {
      @Nonnull
      @Override
      protected K compute() {
        return value.create();
      }
    };
  }

  @Nonnull
  public static <K> AtomicNotNullLazyValue<K> createValue(@Nonnull final Supplier<K> value) {
    return new AtomicNotNullLazyValue<K>() {
      @Nonnull
      @Override
      protected K compute() {
        return value.get();
      }
    };
  }

  private volatile T myValue;

  @Override
  @Nonnull
  public final T getValue() {
    T value = myValue;
    if (value != null) {
      return value;
    }
    synchronized (this) {
      value = myValue;
      if (value == null) {
        myValue = value = compute();
      }
    }
    return value;
  }
}