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
package consulo.application.util;

import consulo.annotation.DeprecationInfo;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Compute-once keep-forever lazy value.
 * Thread-safe version: {@link AtomicNotNullLazyValue}.
 * Clearable version: {@link ClearableLazyValue}.
 *
 * @author peter
 */
@Deprecated
@DeprecationInfo("Use LazyValue")
public abstract class NotNullLazyValue<T> implements Supplier<T> {
  private @Nullable T myValue;

  protected abstract T compute();

  public T getValue() {
    T result = myValue;
    if (result == null) {
      RecursionGuard.StackStamp stamp = RecursionManager.markStack();
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

  public static <T> NotNullLazyValue<T> createConstantValue(T value) {
    return new NotNullLazyValue<>() {
      @Override
      protected T compute() {
        return value;
      }
    };
  }

  public static <T> NotNullLazyValue<T> createValue(final Supplier<T> value) {
    return new NotNullLazyValue<T>() {
      
      @Override
      protected T compute() {
        return Objects.requireNonNull(value.get());
      }
    };
  }
}