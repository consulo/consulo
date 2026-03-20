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

import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;

/**
 *  @author dsl
 */
@Deprecated
@DeprecationInfo("Use Supplier")
@FunctionalInterface
public interface Computable <T> extends Supplier<T> {
  @Nullable T compute();

  @Override
  default @Nullable T get() {
    return compute();
  }

  class PredefinedValueComputable<T> implements Computable<T> {
    private final @Nullable T myValue;

    public PredefinedValueComputable(@Nullable T value) {
      myValue = value;
    }

    @Override
    public @Nullable T compute() {
      return myValue;
    }
  }

  abstract class NotNullCachedComputable<T> implements Computable<T> {
    private @Nullable T myValue;

    protected abstract T internalCompute();

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
    private @Nullable Object myValue = null;

    protected abstract @Nullable T internalCompute();

    @Override
    public final @Nullable T compute() {
      if (myValue == null) {
        T value = internalCompute();
        myValue = value != null ? value : NULL_VALUE;
      }
      return myValue != NULL_VALUE ? (T)myValue : null;
    }
  }
}
