/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
 * Compute-once keep-forever lazy value.
 * Thread-safe version: {@link AtomicNotNullLazyValue}.
 * Clearable version: {@link ClearableLazyValue}.
 *
 * @author peter
 */
public abstract class NotNullLazyValue<T> implements Supplier<T> {
  private static final RecursionGuard ourGuard = RecursionManager.createGuard("NotNullLazyValue");
  private T myValue;

  @Nonnull
  protected abstract T compute();

  @Nonnull
  public T getValue() {
    T result = myValue;
    if (result == null) {
      RecursionGuard.StackStamp stamp = ourGuard.markStack();
      result = compute();
      if (stamp.mayCacheNow()) {
        myValue = result;
      }
    }
    return result;
  }

  @Override
  public T get() {
    return getValue();
  }

  public boolean isComputed() {
    return myValue != null;
  }

  @Nonnull
  public static <T> NotNullLazyValue<T> createConstantValue(@Nonnull final T value) {
    return new NotNullLazyValue<T>() {
      @Nonnull
      @Override
      protected T compute() {
        return value;
      }
    };
  }

  @Nonnull
  public static <T> NotNullLazyValue<T> createValue(@Nonnull final NotNullFactory<T> value) {
    return new NotNullLazyValue<T>() {
      @Nonnull
      @Override
      protected T compute() {
        return value.create();
      }
    };
  }

  @Nonnull
  public static <T> NotNullLazyValue<T> createValue(@Nonnull final Supplier<T> value) {
    return new NotNullLazyValue<T>() {
      @Nonnull
      @Override
      protected T compute() {
        return value.get();
      }
    };
  }
}