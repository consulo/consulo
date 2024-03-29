// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * NOTE: Assumes that values computed by different threads are equal and interchangeable
 * and readers should be ready to get different instances on different invocations of the {@link #getValue()}
 *
 * @author peter
 */
@Deprecated
@DeprecationInfo("Use LazyValue")
public abstract class VolatileNullableLazyValue<T> extends NullableLazyValue<T> {
  private volatile boolean myComputed;
  @Nullable
  private volatile T myValue;

  @Override
  @Nullable
  protected abstract T compute();

  @Override
  @Nullable
  public T getValue() {
    boolean computed = myComputed;
    T value = myValue;
    if (!computed) {
      RecursionGuard.StackStamp stamp = RecursionManager.markStack();
      value = compute();
      if (stamp.mayCacheNow()) {
        myValue = value;
        myComputed = true;
      }
    }
    return value;
  }

  public void drop() {
    myComputed = false;
    myValue = null;
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Nonnull
  public static <T> VolatileNullableLazyValue<T> createValue(@Nonnull final Supplier<? extends T> value) {
    return new VolatileNullableLazyValue<T>() {

      @Nullable
      @Override
      protected T compute() {
        return value.get();
      }
    };
  }

}
