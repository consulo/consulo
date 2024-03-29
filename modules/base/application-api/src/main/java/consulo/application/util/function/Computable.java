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
package consulo.application.util.function;

import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 *  @author dsl
 */
@FunctionalInterface
@Deprecated
@DeprecationInfo("Use Supplier")
public interface Computable <T> extends Supplier<T> {

  T compute();

  @Override
  default T get() {
    return compute();
  }

  class PredefinedValueComputable<T> implements Computable<T> {

    private final T myValue;

    public PredefinedValueComputable(@Nullable T value) {
      myValue = value;
    }

    @Override
    public T compute() {
      return myValue;
    }
  }

  abstract class NotNullCachedComputable<T> implements Computable<T> {
    private T myValue;

    @Nonnull
    protected abstract T internalCompute();

    @Nonnull
    @Override
    public final T compute() {
      if (myValue == null) {
        myValue = internalCompute();
      }
      return myValue;
    }
  }

  abstract class NullableCachedComputable<T> implements Computable<T> {
    private static final Object NULL_VALUE = new Object();
    private Object myValue;

    @Nullable
    protected abstract T internalCompute();

    @Nullable
    @Override
    public final T compute() {
      if (myValue == null) {
        final T value = internalCompute();
        myValue = value != null ? value : NULL_VALUE;
      }
      return myValue != NULL_VALUE ? (T)myValue : null;
    }
  }
}
